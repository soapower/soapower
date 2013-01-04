package models

import java.util.{ GregorianCalendar, Calendar, Date }
import play.api.db._
import play.api.Play.current
import play.api._
import play.api.cache._
import play.api.libs.json._
import play.api.http._
import anorm._
import anorm.SqlParser._
import java.sql.Connection
import collection.mutable.Set
import java.io.{ByteArrayOutputStream, BufferedOutputStream}
import java.util.zip.{Deflater, Inflater, GZIPOutputStream}
import java.nio.charset.Charset

case class RequestData(
  var id: Pk[Long],
  sender: String,
  var soapAction: String,
  environmentId: Long,
  serviceId: Long,
  var request: String,
  var requestHeaders: Map[String, String],
  startTime: Date,
  var response: String,
  var responseHeaders: Map[String, String],
  var timeInMillis: Long,
  var status: Int,
  var purged: Boolean) {

  def this(sender: String, soapAction: String, environnmentId: Long, serviceId: Long) =
    this(null, sender, soapAction, environnmentId, serviceId, null, null, new Date, null, null, -1, -1, false)

  /**
   * Add soapAction in cache if neccessary.
   */
  def storeSoapActionAndStatusInCache() {
    if (!RequestData.soapActionOptions.exists(p => p._1 == soapAction)) {
      Logger.info("SoapAction " + soapAction + " not found in cache : add to cache")
      val inCache = RequestData.soapActionOptions ++ (List((soapAction, soapAction)))
      Cache.set(RequestData.keyCacheSoapAction, inCache)
    }
    if (!RequestData.statusOptions.exists(p => p._1 == status)) {
      Logger.info("Status " + soapAction + " not found in cache : add to cache")
      val inCache = RequestData.statusOptions ++ (List((status, status)))
      Cache.set(RequestData.keyCacheStatusOptions, inCache)
    }
  }
}

object RequestData {

  val keyCacheSoapAction = "soapaction-options"
  val keyCacheStatusOptions = "status-options"
  val keyCacheMinStartTime = "minStartTime"

  /**
   * Parse a RequestData from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("request_data.id") ~
      str("request_data.sender") ~
      str("request_data.soapAction") ~
      long("request_data.environmentId") ~
      long("request_data.serviceId") ~
      get[Date]("request_data.startTime") ~
      long("request_data.timeInMillis") ~
      int("request_data.status") ~
      str("request_data.purged") map {
        case id ~ sender ~ soapAction ~ environnmentId ~ serviceId ~ startTime ~ timeInMillis ~ status ~ purged =>
          RequestData(id, sender, soapAction, environnmentId, serviceId, null, null, startTime, null, null, timeInMillis, status, (purged == "true"))
      }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "soapAction" -> 2, "startTime" -> 3, "timeInMillis" -> 4, "environmentName" -> 5)

  val csvKey = "requestDataStat";

  /**
   * Csv format.
   */
  val csv = {
    get[Pk[Long]]("request_data.id") ~
      get[String]("request_data.soapAction") ~
      get[Date]("request_data.startTime") ~
      get[Long]("request_data.timeInMillis") ~
      get[String]("environment.name") map {
      case id ~ soapAction ~ startTime ~ timeInMillis ~ environmentName =>
        id + ";" + soapAction + ";" + startTime + ";" + timeInMillis + ";" + environmentName + "\n"
    }
  }

  /**
   * Get All RequestData, csv format.
   * @return List of RequestData, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * " +
      " from request_data left join environment on environmentId = environment.id " +
      " where isStats = 'true' ").as(RequestData.csv. *)
  }

  /**
   * Parse required parts of a RequestData from a ResultSet in order to replay the request
   */
  val forReplay = {
    get[Pk[Long]]("request_data.id") ~
      str("request_data.sender") ~
      str("request_data.soapAction") ~
      long("request_data.environmentId") ~
      long("request_data.serviceId") ~
      str("request_data.request") ~
      str("request_data.requestHeaders") map {
        case id ~ sender ~ soapAction ~ environnmentId ~ serviceId ~ request ~ requestHeaders =>
          val headers = headersFromString(requestHeaders)
          new RequestData(id, sender, soapAction, environnmentId, serviceId, request, headers, null, null, null, -1, -1, false)
      }
  }

  /**
   * Construct the Map[String, String] needed to fill a select options set.
   */
  def soapActionOptions: Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      Cache.getOrElse[Seq[(String, String)]](keyCacheSoapAction) {
        Logger.debug("RequestData.SoapAction not found in cache: loading from db")
        SQL("select distinct(soapAction) from request_data order by soapAction asc").as((get[String]("soapAction") ~ get[String]("soapAction")) *).map(flatten)
      }
  }

  /**
   * Construct the Map[String, String] needed to fill a select options set.
   */
  def statusOptions: Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      Cache.getOrElse[Seq[(String, String)]](keyCacheStatusOptions) {
        Logger.debug("RequestData.status not found in cache: loading from db")
        SQL("select distinct(status) from request_data").as(get[Int]("status") *).map(c => c.toString -> c.toString)
      }
  }

  /**
   * find Min Date.
   */
  def getMinStartTime: Option[Date] = DB.withConnection {
    implicit connection =>
      Cache.getOrElse[Option[Date]](keyCacheMinStartTime) {
        Logger.debug("MinStartTime not found in cache: loading from db")
        SQL("select min(startTime) as startTimeMin from request_data").as(scalar[Option[Date]].single)
      }
  }

  /**
   * Insert a new RequestData.
   *
   * @param requestData the requestData
   */
  def insert(requestData: RequestData) = {
    var xmlRequest : Array[Byte] = null
    var xmlResponse : Array[Byte] = null

    val environment = Environment.findById(requestData.environmentId).get
    val date = new Date()
    val gcal = new GregorianCalendar()
    gcal.setTime(date)
    gcal.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format

    if (environment.hourRecordXmlDataMin <= gcal.get(Calendar.HOUR_OF_DAY) &&
      environment.hourRecordXmlDataMax > gcal.get(Calendar.HOUR_OF_DAY)) {
      xmlRequest = compressString(requestData.request)
      xmlResponse = compressString(requestData.response)
    } else {
      val msg = "Xml Data not recording. Record between " + environment.hourRecordXmlDataMin + "h to " + environment.hourRecordXmlDataMax + "h for this environment."
      xmlRequest = compressString(msg)
      xmlResponse = compressString(msg)
      Logger.debug(msg)
    }

    try {
      DB.withConnection {
        implicit connection =>
          SQL(
            """
            insert into request_data 
              (sender, soapAction, environmentId, serviceId, request, requestHeaders, startTime, response, responseHeaders, timeInMillis, status) values (
              {sender}, {soapAction}, {environmentId}, {serviceId}, {request}, {requestHeaders}, {startTime}, {response}, {responseHeaders}, {timeInMillis}, {status}
            )
            """).on(
              'sender -> requestData.sender,
              'soapAction -> requestData.soapAction,
              'environmentId -> requestData.environmentId,
              'serviceId -> requestData.serviceId,
              'request -> xmlRequest,
              'requestHeaders -> headersToString(requestData.requestHeaders),
              'startTime -> requestData.startTime,
              'response -> xmlResponse,
              'responseHeaders -> headersToString(requestData.responseHeaders),
              'timeInMillis -> requestData.timeInMillis,
              'status -> requestData.status).executeInsert()
      } match {
        case Some(long) => long // The Primary Key
        case None       => -1
      }

    } catch {
      case e: Exception => Logger.error("Error during insertion of RequestData ", e)
      -1
    }
  }

  /**
   * Insert a new RequestData.
   */
  def insertStats(environmentId : Long, soapAction: String, startTime : Date, timeInMillis : Long) = {

     try {

      DB.withConnection {
        implicit connection =>
          val gcal = new GregorianCalendar()
          gcal.setTimeInMillis(startTime.getTime + UtilDate.v1d)

          val purgedStats = SQL(
            """
            delete from request_data
            where isStats = 'true'
            and startTime >= {startTime} and startTime < {startTime}
            and soapAction = {soapAction}
            """
          ).on(
            'soapAction -> soapAction,
            'environmentId -> environmentId,
            'startTime -> startTime).executeUpdate()

          Logger.debug("Purged " + purgedStats + " existing stats for:" + environmentId + " and startTime:" + startTime)


          SQL(
            """
            insert into request_data
              (sender, soapAction, environmentId, request, requestHeaders, startTime, response, responseHeaders, timeInMillis, status, isStats) values (
              '', {soapAction}, {environmentId}, '', '', {startTime}, '', '', {timeInMillis}, 200, true
            )
            """).on(
            'soapAction -> soapAction,
            'environmentId -> environmentId,
            'startTime -> startTime,
            'timeInMillis -> timeInMillis).executeUpdate()
      }
    } catch {
      case e: Exception => Logger.error("Error during insertion of RequestData Stats", e)
    }
  }

  /**
   * Delete all request data !
   */
  /*def deleteAll() {
    DB.withConnection {
      implicit connection =>
        SQL("delete from request_data").executeUpdate()
    }
    Cache.remove(keyCacheSoapAction)
    Cache.remove(keyCacheStatusOptions)
  }*/

  /**
   * Delete XML data (request & reponse) between min and max date
   * @param environmentIn environmement or "" / all if all
   * @param minDate min date
   * @param maxDate max date
   * @param user use who delete the data : admin or akka
   */
  def deleteRequestResponse(environmentIn: String, minDate: Date, maxDate: Date, user: String): Int = {
    Logger.debug("Environment:" + environmentIn + " mindate:" + minDate.getTime + " maxDate:" + maxDate.getTime)
    Logger.debug("EnvironmentSQL:" + sqlAndEnvironnement(environmentIn))

    //val d = new Date()
    //val deleted = "deleted by " + user + " " + d.toString

    DB.withConnection { implicit connection =>
      val purgedRequests = SQL(
        """
            update request_data
            set response = '',
            request = '',
            requestHeaders = '',
            responseHeaders = '',
            purged = 'true'
            where startTime >= {minDate} and startTime <= {maxDate} and purged = 'false' and isStats = 'false'
        """
          + sqlAndEnvironnement(environmentIn)).on(
          'minDate -> minDate,
          'maxDate -> maxDate).executeUpdate()

      Cache.remove(keyCacheSoapAction)
      Cache.remove(keyCacheStatusOptions)
      purgedRequests
    }
  }

  /**
   * Delete entries between min and max date
   * @param environmentIn environmement or "" / all if all
   * @param minDate min date
   * @param maxDate max date
   */
  def delete(environmentIn: String, minDate: Date, maxDate: Date) : Int = {
    DB.withConnection {
      implicit connection =>
      val deletedRequests = SQL(
        """
            delete from request_data
            where startTime >= {minDate} and startTime < {maxDate}
            and isStats = false
        """
          + sqlAndEnvironnement(environmentIn)).on(
            'minDate -> minDate,
            'maxDate -> maxDate).executeUpdate()
      Cache.remove(keyCacheSoapAction)
      Cache.remove(keyCacheStatusOptions)
      deletedRequests
    }
  }

  /*
  * Get All RequestData, used for testing only
  */
  def findAll(): List[RequestData] = DB.withConnection {
    implicit c =>
      SQL("select * from request_data").as(RequestData.simple *)
  }

  /**
   * Return a page of RequestData
   * @param environmentIn name of environnement, "all" default
   * @param soapAction soapAction, "all" default
   * @param minDate Min Date
   * @param maxDate Max Date
   * @param status Status
   * @param offset offset in search
   * @param pageSize size of line in one page
   * @param filterIn filter on soapAction. Usefull only is soapActionIn = "all"
   * @return
   */
  def list(environmentIn: String, soapAction: String, minDate: Date, maxDate: Date, status: String, offset: Int = 0, pageSize: Int = 10, filterIn: String = "%"): Page[(RequestData)] = {
    val filter = "%" + filterIn + "%"

    // Convert dates... bad perf anorm ?
    val g = new GregorianCalendar()
    g.setTime(minDate)
    val min = UtilDate.formatDate(g)
    g.setTime(maxDate)
    val max = UtilDate.formatDate(g)

    var whereClause = "where startTime >= '" + min + "' and startTime <= '" + max + "'"
    if (status != "all") whereClause += " and status = {status}"
    if (soapAction != "all") whereClause += " and soapAction = {soapAction}"
    if (filterIn != "%" && filterIn.trim != "") whereClause += " and soapAction like {filter}"

    whereClause += " and isStats = 'false' "

    whereClause += sqlAndEnvironnement(environmentIn)

    val params: Array[(Any, anorm.ParameterValue[_])] = Array(
      'pageSize -> pageSize,
      'offset -> offset,
      'soapAction -> soapAction,
      //'minDate -> minDate,
      //'maxDate -> maxDate,
      'status -> status,
      'filter -> filter)

    DB.withConnection {
      implicit connection =>
      val requestTimeInMillis = System.currentTimeMillis
        val requests = SQL(
          "select id, sender, soapAction, environmentId, serviceId, " +
            " startTime, timeInMillis, status, purged from request_data "
            + whereClause +
            " order by request_data.id " +
            " desc limit {offset}, {pageSize}").on(params: _*).as(RequestData.simple *)
        val middle = System.currentTimeMillis
          Logger.debug("Middle : "+ (System.currentTimeMillis - requestTimeInMillis))

        val totalRows = SQL(
          "select count(id) from request_data " + whereClause).on(params: _*).as(scalar[Long].single)
        Logger.debug("End : "+ (System.currentTimeMillis - middle))

        Page(requests, -1, offset, totalRows)
    }
  }

  /**
   * Load reponse times for given parameters
   */
  def findResponseTimes(environmentIn: String, soapAction: String, minDate: Date, maxDate: Date, status: String, statsOnly: Boolean): List[(Long, String, Date, Long)] = {

    var whereClause = "where startTime >= {minDate} and startTime <= {maxDate}"
    if (status != "all") whereClause += " and status = {status}"
    if (soapAction != "all") whereClause += " and soapAction = {soapAction}"
    whereClause += sqlAndEnvironnement(environmentIn)

    val params: Array[(Any, anorm.ParameterValue[_])] = Array(
      'status -> status,
      'minDate -> minDate,
      'maxDate -> maxDate,
      'soapAction -> soapAction)

    var sql = "select environmentId, soapAction, startTime, timeInMillis from request_data " + whereClause

    if (statsOnly) {
      sql  += " and isStats = 'true' "
    }
    sql += " order by request_data.id asc"

    DB.withConnection { implicit connection =>
      // explainPlan(sql, params: _*)
      SQL(sql)
        .on(params: _*)
        .as(get[Long]("environmentId") ~ get[String]("soapAction") ~ get[Date]("startTime") ~ get[Long]("timeInMillis") *)
        .map(flatten)
    }
  }

  def loadAvgResponseTimesByAction(environmentName: String, minDate: Date, maxDate: Date, withStats : Boolean): List[(String, Long)] = {
    DB.withConnection { implicit connection =>

      var whereClause = ""
      if (!withStats) whereClause = " and isStats = 'false' "

      Logger.debug("Load Stats with env:" + environmentName)
      whereClause += sqlAndEnvironnement(environmentName)

      val responseTimes = SQL(
        """
          select soapAction, timeInMillis
          from request_data
          where status=200
          and startTime >= {minDate} and startTime <= {maxDate}
        """ + whereClause + """
          order by timeInMillis asc
        """)
        .on(
          'minDate -> minDate,
          'maxDate -> maxDate)
        .as(get[String]("soapAction") ~ get[Long]("timeInMillis") *)
        .map(flatten)

      val avgTimesByAction = responseTimes.groupBy(_._1).mapValues { e =>
        val times = e.map(_._2).toList
        val ninePercentiles = times.slice(0, times.size * 9 / 10)
        if (ninePercentiles.size > 0) {
          ninePercentiles.sum / ninePercentiles.size
        } else if (times.size == 1) {
          times.head
        } else {
          -1
        }
      }
      avgTimesByAction.toList.filterNot(_._2 == -1).sortBy(_._1)
    }
  }

  /**
   * Find all day before today, for environment given and state = 200.
   * @param environmentId environment id
   * @return list of unique date
   */
  def findDayNotCompileStats(environmentId: Long) : List[Date] = {
    DB.withConnection { implicit connection =>

      val gcal = new GregorianCalendar
      val today = new GregorianCalendar(gcal.get(Calendar.YEAR),gcal.get(Calendar.MONTH),gcal.get(Calendar.DATE))

      val startTimes = SQL(
        """
          select startTime from request_data
          where environmentId={environmentId} and status=200 and isStats = 'false' and startTime < {today}
        """
      ).on('environmentId -> environmentId, 'today -> today.getTime)
       .as(get[Date]("startTime") *).toList

      val uniqueStartTimePerDay : Set[Date] = Set()
      startTimes.foreach{(t) =>
        gcal.setTimeInMillis(t.getTime)
        val ccal = new GregorianCalendar(gcal.get(Calendar.YEAR),gcal.get(Calendar.MONTH),gcal.get(Calendar.DATE))
        uniqueStartTimePerDay += ccal.getTime
      }
      uniqueStartTimePerDay.toList
    }
  }

  implicit def rowToByteArray: Column[Array[Byte]] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
    case data: Array[Byte] => Right(data)
    case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Byte Array for column " + qualified))
    }
  }

  def bytes(columnName: String): RowParser[Array[Byte]] = get[Array[Byte]](columnName)(implicitly[Column[Array[Byte]]])

  def load(id: Long): RequestData = {
    DB.withConnection { implicit connection =>
      SQL("select id, sender, soapAction, environmentId, serviceId, request, requestHeaders from request_data where id= {id}")
        .on('id -> id).as(RequestData.forReplay.single)
    }
  }

  def loadRequest(id: Long): String = {
    DB.withConnection { implicit connection =>
      val request = SQL("select request from request_data where id= {id}").on('id -> id).as(bytes("request").singleOpt)
      decompressString(request.get)
    }
  }

  def loadResponse(id: Long): Option[String] = {
    DB.withConnection { implicit connection =>
      val response = SQL("select response from request_data where id = {id}").on('id -> id).as(bytes("response").singleOpt)
      // TODO Fix if no response
      Some(decompressString(response.get))
    }
  }

  private def sqlAndEnvironnement(environmentIn: String): String = {
    var environment = ""

    // search by name
    if (environmentIn != "all" && Environment.options.exists(t => t._2 == environmentIn))
      environment = "and request_data.environmentId = " + Environment.options.find(t => t._2 == environmentIn).get._1

    // search by id
    if (environment == "" && Environment.options.exists(t => t._1 == environmentIn))
      environment = "and request_data.environmentId = " + Environment.options.find(t => t._1 == environmentIn).get._1

    environment
  }

  private def headersToString(headers: Map[String, String]): String = {
    if (headers != null) {
      headers.foldLeft("") { (string, header) => string + header._1 + " -> " + header._2 + "\n" }
    } else {
      ""
    }
  }

  private def headersFromString(headersAsStr: String): Map[String, String] = {
    def headers = headersAsStr.split("\n").collect {
      case header =>
        val parts = header.split(" -> ")
        (parts(0), parts.tail.mkString(""))
    }
    headers.toMap
  }

  // use by Json : from scala to json
  implicit object RequestDataWrites extends Writes[RequestData] {

    def writes(o: RequestData): JsValue = {
      val dlRequestUrl = "/download/request/" + o.id
      val dlResponseUrl = "/download/response/" + o.id

      var requestDownloadLinks = "-"
      var responseDownloadLinks = "-"
      if (!o.purged) {
        requestDownloadLinks =
          "<a href='" + dlRequestUrl + "?asFile=true' title='Download'><i class='icon-file'></i></a> " +
            "<a target='_blank' href='" + dlRequestUrl + "' title='Open in new tab'><i class='icon-eye-open'></i></a>"
        responseDownloadLinks =
          "<a href='" + dlResponseUrl + "?asFile=true' title='Download'><i class='icon-file'></i></a> " +
            "<a target='_blank' href='" + dlResponseUrl + "' title='Open in new tab'><i class='icon-eye-open'></i></a>"
      }

      JsObject(
        List("0" -> JsString(status(o.status)),
          "1" -> JsString(Environment.options.find(t => t._1 == o.environmentId.toString).get._2),
          "2" -> JsString(o.sender),
          "3" -> JsString(soapAction(o)),
          "4" -> JsString(UtilDate.getDateFormatees(o.startTime)),
          "5" -> JsString(o.timeInMillis.toString),
          "6" -> JsString(requestDownloadLinks),
          "7" -> JsString(responseDownloadLinks),
          "8" -> JsString("<a href='#' class='replay' data-request-id='" + o.id + "'><i class='icon-refresh'></i></a>")))
    }

    private def status(status: Int): String = {
      if (status == Status.OK) {
        "<span class='label label-success'>" + status.toString + "</span>"
      } else {
        "<span class='label label-important'>" + status.toString + "</span>"
      }
    }

    private def soapAction(o: RequestData): String = {
      if (o.serviceId > 0) {
        val s = Service.findById(o.serviceId).get
        "<a class='popSoapAction' href='#' rel='tooltip' title='Local: " + s.localTarget + " Remote: " + s.remoteTarget + "'>" + o.soapAction + "</a>"
      } else {
        o.soapAction
      }
    }
  }

  private def explainPlan(sql: String, params: (Any, anorm.ParameterValue[_])*)(implicit connection: Connection) {
    val plan = SQL("explain plan for " + sql).on(params: _*).resultSet()
    while (plan.next) {
      println(plan.getString(1))
    }
  }

  /**
   * Upload a csvLine => insert requestDataStat.
   *
   * @param csvLine line in csv file
   * @return nothing
   */
  def upload(csvLine: String) = {

    val dataCsv = csvLine.split(";")

    if (dataCsv.size != csvTitle.size)
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")

    if (dataCsv(csvTitle.get("key").get) != csvKey) {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    } else {
      val environmentName = dataCsv(csvTitle.get("environmentName").get).trim
      val e = Environment.findByName(environmentName)

      e.map {
        environment =>
          insertStats(environment.id.get, dataCsv(csvTitle.get("soapAction").get).trim, UtilDate.parse(dataCsv(csvTitle.get("startTime").get).trim), dataCsv(csvTitle.get("timeInMillis").get).toLong)
      }.getOrElse {
        Logger.warn("Warning : Environment " + environmentName + " unknown")
        throw new Exception("Warning : Environment " + environmentName + " already exist")
      }
    }
  }

  def compressString(data: String): Array[Byte] = {
    val deflater = new Deflater()
    deflater.setInput(data.getBytes(Charset.forName("utf-8")))
    deflater.finish()
    //TODO may oveflow, find better solution
    var output = new Array[Byte](100000)
    deflater.deflate(output, 0, output.length)
    output
  }

  def decompressString(input: Array[Byte]): String = {

    var result: String = null

    val inflater = new Inflater()
    inflater.setInput(input, 0, input.length)

    //TODO may oveflow, find better solution
    var output = new Array[Byte](100000)

    val resultLength = inflater.inflate(output)
    inflater.end()

    result = new String(output, 0, resultLength, Charset.forName("utf-8"))

    return result;
  }
}
