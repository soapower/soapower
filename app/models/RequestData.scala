package models

import java.util.{GregorianCalendar, Calendar, Date}
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
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import java.nio.charset.Charset
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}

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
                        var purged: Boolean,
                        var isMock: Boolean) {

  var responseBytes: Array[Byte] = null

  def this(sender: String, soapAction: String, environnmentId: Long, serviceId: Long) =
    this(null, sender, soapAction, environnmentId, serviceId, null, null, new Date, null, null, -1, -1, false, false)

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

  implicit def rowToByteArray: Column[Array[Byte]] = Column.nonNull {
    (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case data: Array[Byte] => Right(data)
        case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Byte Array for column " + qualified))
      }
  }

  /**
   * Anorm Byte conversion
   */
  def bytes(columnName: String): RowParser[Array[Byte]] = get[Array[Byte]](columnName)(implicitly[Column[Array[Byte]]])

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
      str("request_data.purged") ~
      str("request_data.isMock") map {
      case id ~ sender ~ soapAction ~ environnmentId ~ serviceId ~ startTime ~ timeInMillis ~ status ~ purged ~ isMock =>
        RequestData(id, sender, soapAction, environnmentId, serviceId, null, null, startTime, null, null, timeInMillis, status, (purged == "true"), (isMock == "true"))
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
   * @return List of RequestData, csv format, except mock
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("SELECT *  FROM  request_data, environment WHERE environmentId = environment.id and isStats = 'true';").as(RequestData.csv.*)
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
      bytes("request_data.request") ~
      str("request_data.requestHeaders") map {
      case id ~ sender ~ soapAction ~ environnmentId ~ serviceId ~ request ~ requestHeaders =>
        val headers = UtilConvert.headersFromString(requestHeaders)
        new RequestData(id, sender, soapAction, environnmentId, serviceId, uncompressString(request), headers, null, null, null, -1, -1, false, false)
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
  def statusOptions: Seq[(Int, Int)] = DB.withConnection {
    implicit connection =>
      Cache.getOrElse[Seq[(Int, Int)]](keyCacheStatusOptions) {
        Logger.debug("RequestData.status not found in cache: loading from db")
        SQL("select distinct(status) from request_data").as(get[Int]("status") *).map(c => c -> c)
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
  def insert(requestData: RequestData): Long = {
    var xmlRequest: Array[Byte] = null
    var xmlResponse: Array[Byte] = null

    val environment = Environment.findById(requestData.environmentId).get
    val service = Service.findById(requestData.serviceId).get
    val date = new Date()
    val gcal = new GregorianCalendar()
    gcal.setTime(date)
    gcal.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format

    if (!service.recordData || !environment.recordData) {
      Logger.debug("Data not recording for this service or this environment")
      return -1
    } else if (!service.recordXmlData) {
      val msg = "Xml Data not recording for this service. See Admin."
      xmlRequest = compressString(msg)
      xmlResponse = compressString(msg)
      Logger.debug(msg)
    } else if (!environment.recordXmlData) {
      val msg = "Xml Data not recording for this environment. See Admin."
      xmlRequest = compressString(msg)
      xmlResponse = compressString(msg)
      Logger.debug(msg)
    } else if (requestData.status != 200 || (
      environment.hourRecordXmlDataMin <= gcal.get(Calendar.HOUR_OF_DAY) &&
        environment.hourRecordXmlDataMax > gcal.get(Calendar.HOUR_OF_DAY))) {
      // Record XML Data if it is a soap fault (status != 200) or
      // if we can record data with environment's configuration (hours of recording)
      xmlRequest = compressString(requestData.request)
      def transferEncodingResponse = requestData.responseHeaders.filter {
        _._1 == HeaderNames.CONTENT_ENCODING
      }

      transferEncodingResponse.get(HeaderNames.CONTENT_ENCODING) match {
        case Some("gzip") =>
          Logger.debug("Response in gzip Format")
          xmlResponse = requestData.responseBytes
        case _ =>
          Logger.debug("Response in plain Format")
          xmlResponse = compressString(requestData.response)
      }
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
              (sender, soapAction, environmentId, serviceId, request, requestHeaders, startTime, response, responseHeaders, timeInMillis, status, isMock) values (
              {sender}, {soapAction}, {environmentId}, {serviceId}, {request}, {requestHeaders}, {startTime}, {response}, {responseHeaders}, {timeInMillis}, {status}, {isMock}
            )
            """).on(
            'sender -> requestData.sender,
            'soapAction -> requestData.soapAction,
            'environmentId -> requestData.environmentId,
            'serviceId -> requestData.serviceId,
            'request -> xmlRequest,
            'requestHeaders -> UtilConvert.headersToString(requestData.requestHeaders),
            'startTime -> requestData.startTime,
            'response -> xmlResponse,
            'responseHeaders -> UtilConvert.headersToString(requestData.responseHeaders),
            'timeInMillis -> requestData.timeInMillis,
            'status -> requestData.status,
            'isMock -> requestData.isMock
            ).executeInsert()
      } match {
        case Some(long) => long // The Primary Key
        case None => -1
      }

    } catch {
      case e: Exception => Logger.error("Error during insertion of RequestData ", e)
        -1
    }
  }

  /**
   * Insert a new RequestData.
   */
  def insertStats(environmentId: Long, soapAction: String, startTime: Date, timeInMillis: Long) = {

    try {

      DB.withConnection {
        implicit connection =>

          val purgedStats = SQL(
            """
            delete from request_data
            where isStats = 'true'
            and environmentId = {environmentId}
            and startTime >= {startTime} and startTime <= {startTime}
            and soapAction = {soapAction}
            """
          ).on(
            'soapAction -> soapAction,
            'environmentId -> environmentId,
            'startTime -> startTime).executeUpdate()

          Logger.debug("Purged " + purgedStats + " existing stats for:" + environmentId + " soapAction: " + soapAction + " and startTime:" + startTime)

          SQL(
            """
            insert into request_data
              (sender, soapAction, environmentId, request, requestHeaders, startTime, response, responseHeaders, timeInMillis, status, isStats, isMock) values (
              '', {soapAction}, {environmentId}, '', '', {startTime}, '', '', {timeInMillis}, 200, 'true', 'false'
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

    DB.withConnection {
      implicit connection =>
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
  def delete(environmentIn: String, minDate: Date, maxDate: Date): Int = {
    DB.withConnection {
      implicit connection =>
        val deletedRequests = SQL(
          """
            delete from request_data
            where startTime >= {minDate} and startTime < {maxDate}
            and isStats = 'false'
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
   * @param groupName group name
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
  def list(groupName: String, environmentIn: String, soapAction: String, minDate: Date, maxDate: Date, status: String, offset: Int = 0, pageSize: Int = 10, filterIn: String = "%"): Page[(RequestData)] = {
    val filter = "%" + filterIn + "%"

    // Convert dates... bad perf anorm ?
    val g = new GregorianCalendar()
    g.setTime(minDate)
    val min = UtilDate.formatDate(g)
    g.setTime(maxDate)
    val max = UtilDate.formatDate(g)

    var whereClause = " where startTime >= '" + min + "' and startTime <= '" + max + "'"
    whereClause += sqlAndStatus(status)

    if (soapAction != "all") whereClause += " and soapAction = {soapAction}"
    if (filterIn != "%" && filterIn.trim != "") whereClause += " and soapAction like {filter}"

    whereClause += " and isStats = 'false' "
    whereClause += sqlAndEnvironnement(environmentIn)
    val pair = sqlFromAndGroup(groupName, environmentIn)
    val fromClause = "from request_data " + pair._1
    whereClause += pair._2

    Logger.debug("Offset:" + offset + " pageSize x pageSize:" + (offset * pageSize) + " offset:" + offset);

    val params: Array[(Any, anorm.ParameterValue[_])] = Array(
      'pageSize -> pageSize,
      'offset -> offset * pageSize,
      'soapAction -> soapAction,
      'filter -> filter)

    val sql = "select request_data.id, sender, soapAction, environmentId, serviceId, " +
      " startTime, timeInMillis, status, purged, isMock " + fromClause + whereClause +
      " order by startTime " +
      " desc limit {offset}, {pageSize}"

    Logger.debug("SQL (list) ====> " + sql)
    DB.withConnection {
      implicit connection =>
        val requestTimeInMillis = System.currentTimeMillis
        val requests = SQL(sql).on(params: _*).as(RequestData.simple *)
        val middle = System.currentTimeMillis
        Logger.debug("Middle : " + (System.currentTimeMillis - requestTimeInMillis))

        val totalRows = SQL(
          "select count(request_data.id) " + fromClause + whereClause).on(params: _*).as(scalar[Long].single)
        Logger.debug("End : " + (System.currentTimeMillis - middle) + " totalrows:" + totalRows + " where:" + whereClause);

        Page(requests, -1, offset, totalRows)
    }
  }

  /**
   * Load reponse times for given parameters
   */
  def findResponseTimes(groupName: String, environmentIn: String, soapAction: String, minDate: Date, maxDate: Date, status: String, statsOnly: Boolean): List[(Long, String, Date, Long)] = {

    var whereClause = "where startTime >= {minDate} and startTime <= {maxDate}"
    whereClause += sqlAndStatus(status)
    Logger.debug("DDDDD==>" + whereClause)
    if (soapAction != "all") whereClause += " and soapAction = {soapAction}"
    whereClause += sqlAndEnvironnement(environmentIn)

    val pair = sqlFromAndGroup(groupName, environmentIn)
    val fromClause = " from request_data " + pair._1
    whereClause += pair._2

    val params: Array[(Any, anorm.ParameterValue[_])] = Array(
      'minDate -> minDate,
      'maxDate -> maxDate,
      'soapAction -> soapAction)

    var sql = "select environmentId, soapAction, startTime, timeInMillis " + fromClause + whereClause

    if (statsOnly) {
      sql += " and isStats = 'true' "
    }
    sql += " order by request_data.id asc"

    Logger.debug("SQL (findResponseTimes, g:" + groupName + ") ====> " + sql)

    DB.withConnection {
      implicit connection =>
      // explainPlan(sql, params: _*)
        SQL(sql)
          .on(params: _*)
          .as(get[Long]("environmentId") ~ get[String]("soapAction") ~ get[Date]("startTime") ~ get[Long]("timeInMillis") *)
          .map(flatten)
    }
  }

  def loadAvgResponseTimesByAction(groupName: String, environmentName: String, status: String, minDate: Date, maxDate: Date, withStats: Boolean): List[(String, Long)] = {
    DB.withConnection {
      implicit connection =>

        var whereClause = " where startTime >= {minDate} and startTime <= {maxDate} "
        whereClause += sqlAndStatus(status)
        if (!withStats) whereClause += " and isStats = 'false' "

        Logger.debug("Load Stats with env:" + environmentName)
        whereClause += sqlAndEnvironnement(environmentName)

        val pair = sqlFromAndGroup(groupName, environmentName)
        val fromClause = " from request_data " + pair._1
        whereClause += pair._2

        val sql = "select soapAction, timeInMillis " + fromClause + whereClause + " order by timeInMillis asc "

        Logger.debug("SQL (loadAvgResponseTimesByAction) ====> " + sql)

        val responseTimes = SQL(sql)
          .on(
          'minDate -> minDate,
          'maxDate -> maxDate)
          .as(get[String]("soapAction") ~ get[Long]("timeInMillis") *)
          .map(flatten)

        val avgTimesByAction = responseTimes.groupBy(_._1).mapValues {
          e =>
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
  def findDayNotCompileStats(environmentId: Long): List[Date] = {
    DB.withConnection {
      implicit connection =>

        val gcal = new GregorianCalendar
        val today = new GregorianCalendar(gcal.get(Calendar.YEAR), gcal.get(Calendar.MONTH), gcal.get(Calendar.DATE))

        val startTimes = SQL(
          """
          select startTime from request_data
          where environmentId={environmentId} and status=200 and isStats = 'false' and isMock = 'false' and startTime < {today}
          """
        ).on('environmentId -> environmentId, 'today -> today.getTime)
          .as(get[Date]("startTime") *).toList

        val uniqueStartTimePerDay: Set[Date] = Set()
        startTimes.foreach {
          (t) =>
            gcal.setTimeInMillis(t.getTime)
            val ccal = new GregorianCalendar(gcal.get(Calendar.YEAR), gcal.get(Calendar.MONTH), gcal.get(Calendar.DATE))
            uniqueStartTimePerDay += ccal.getTime
        }
        uniqueStartTimePerDay.toList
    }
  }

  def load(id: Long): RequestData = {
    DB.withConnection {
      implicit connection =>
        SQL("select id, sender, soapAction, environmentId, serviceId, request, requestHeaders from request_data where id= {id}")
          .on('id -> id).as(RequestData.forReplay.single)
    }
  }

  def loadRequest(id: Long): Option[String] = {
    DB.withConnection {
      implicit connection =>
        val request = SQL("select request from request_data where id= {id}").on('id -> id).as(bytes("request").singleOpt)
        if (request != None) {
          Some(uncompressString(request.get))
        } else {
          None
        }
    }
  }

  def loadResponse(id: Long): Option[String] = {
    DB.withConnection {
      implicit connection =>
        val response = SQL("select response from request_data where id = {id}").on('id -> id).as(bytes("response").singleOpt)
        if (response != None) {
          Some(uncompressString(response.get))
        } else {
          None
        }
    }
  }

  private def sqlAndStatus(statusIn : String) : String = {

    var status = ""
    if (statusIn != "all") {
      if (statusIn.startsWith("NOT_")) {
        status = " and status != " + statusIn.substring(4)
      } else {
        status = " and status = " + statusIn
      }
    }
    status
  }

  private def sqlAndEnvironnement(environmentIn: String): String = {
    var environment = ""

    // search by name
    if (environmentIn != "all" && Environment.optionsAll.exists(t => t._2 == environmentIn))
      environment = " and request_data.environmentId = " + Environment.optionsAll.find(t => t._2 == environmentIn).get._1

    // search by id
    if (environment == "" && Environment.optionsAll.exists(t => t._1 == environmentIn))
      environment = " and request_data.environmentId = " + Environment.optionsAll.find(t => t._1 == environmentIn).get._1

    environment
  }

  private def sqlFromAndGroup(groupName: String, environmentIn: String): (String, String) = {
    var group = ""
    var from = ""

    // search by name
    if (environmentIn == "all" && groupName != "all" && Group.options.exists(t => t._2 == groupName)) {
      group += " and request_data.environmentId = environment.id"
      group += " and environment.groupId = " + Group.options.find(t => t._2 == groupName).get._1
      from = ", environment "
    }

    (from, group)
  }

  // use by Json : from scala to json
  implicit object RequestDataWrites extends Writes[RequestData] {

    def writes(o: RequestData): JsValue = {
      val id = if (o.id != null) o.id.toString else "-1"
      JsObject(
        List(
          "id" -> JsString(id),
          "purged" -> JsString(o.purged.toString),
          "status" -> JsString(o.status.toString),
          "env" -> JsString(Environment.optionsAll.find(t => t._1 == o.environmentId.toString).get._2),
          "sender" -> JsString(o.sender),
          "soapAction" -> JsString(o.soapAction),
          "startTime" -> JsString(UtilDate.getDateFormatees(o.startTime)),
          "time" -> JsString(o.timeInMillis.toString),
          "isMock" -> JsString(o.isMock.toString)))
    }
  }

  private def explainPlan(sql: String, params: (Any, anorm.ParameterValue[_])*)(implicit connection: Connection) {
    val plan = SQL("explain plan for " + sql).on(params: _*).resultSet()
    while (plan.next) {
      Logger.debug(plan.getString(1))
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
          insertStats(environment.id, dataCsv(csvTitle.get("soapAction").get).trim, UtilDate.parse(dataCsv(csvTitle.get("startTime").get).trim), dataCsv(csvTitle.get("timeInMillis").get).toLong)
      }.getOrElse {
        Logger.warn("Warning : Environment " + environmentName + " unknown")
        throw new Exception("Warning : Environment " + environmentName + " already exist")
      }
    }
  }

  /**
   * Compress a string with Gzip
   *
   * @param inputString String to compress
   * @return compressed string
   */
  def compressString(inputString: String): Array[Byte] = {
    try {
      val os = new ByteArrayOutputStream(inputString.length())
      val gos = new GZIPOutputStream(os)
      gos.write(inputString.getBytes(Charset.forName("utf-8")))
      gos.close()
      val compressed = os.toByteArray()
      os.close()
      compressed
    } catch {
      case e: Exception => Logger.error("compressString : Error during compress string")
        inputString.getBytes(Charset.forName("utf-8"))
    }
  }

  /**
   * Uncompress a String with gzip
   *
   * @param compressed compressed String
   * @return clear String
   */
  def uncompressString(compressed: Array[Byte]): String = {
    try {
      val BUFFER_SIZE = 1000
      val is = new ByteArrayInputStream(compressed)
      val gis = new GZIPInputStream(is, BUFFER_SIZE)
      val output = new StringBuilder()
      val data = new Array[Byte](BUFFER_SIZE)
      var ok = true
      while (ok) {
        val bytesRead = gis.read(data)
        ok = bytesRead != -1
        if (ok) output.append(new String(data, 0, bytesRead))
      }
      gis.close()
      is.close()
      output.toString()
    } catch {
      case e: Exception => Logger.error("decompressString : Error during uncompress string:" + e.getStackTraceString)
        new String(compressed, 0, compressed.length, Charset.forName("utf-8"))
    }
  }
}
