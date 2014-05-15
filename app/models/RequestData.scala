package models

import java.util.{TimeZone, GregorianCalendar, Calendar, Date}
import play.api.Play.current
import play.api.libs.json._
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import java.nio.charset.Charset

import java.io.{ByteArrayOutputStream, ByteArrayInputStream}

import scala.concurrent.duration._
import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.{Await, Future}
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.core.commands.{GroupField, Aggregate, RawCommand}
import reactivemongo.api.collections.default.BSONCollection
import play.api.http.HeaderNames
import reactivemongo.bson.BSONDateTime
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONString
import scala.Some
import reactivemongo.bson.BSONLong
import reactivemongo.bson.BSONInteger
import play.api.libs.json.JsObject
import reactivemongo.api.collections.default.BSONCollection
import play.cache.Cache
import java.text.{SimpleDateFormat, DateFormat}
import scala.util.{Success, Failure}
import org.joda.time.DateTime
import reactivemongo.api.indexes.{IndexType, Index}
import scala.collection.mutable.ListBuffer

case class RequestData(_id: Option[BSONObjectID],
                       sender: String,
                       var serviceAction: String,
                       environmentName: String,
                       groupsName: List[String],
                       serviceId: BSONObjectID,
                       var request: String,
                       var requestHeaders: Map[String, String],
                       var contentType: String,
                       var requestCall: Option[String],
                       startTime: DateTime,
                       var response: String,
                       var responseOriginal: Option[String],
                       var responseHeaders: Map[String, String],
                       var timeInMillis: Long,
                       var status: Int,
                       var purged: Boolean,
                       var isMock: Boolean) {

  var responseBytes: Array[Byte] = null

  def this(sender: String, serviceAction: String, environmentName: String, groupsName: List[String], serviceId: BSONObjectID, contentType: String) =
    this(Some(BSONObjectID.generate), sender, serviceAction, environmentName, groupsName, serviceId, null, null, contentType, null, new DateTime(), null, None, null, -1, -1, false, false)

  def toSimpleJson: JsObject = {
    Json.obj(
      "_id" -> _id,
      "status" -> status,
      "contentType" -> contentType,
      "serviceId" -> serviceId,
      "environmentName" -> environmentName,
      "groupsName" -> groupsName,
      "sender" -> sender,
      "serviceAction" -> serviceAction,
      "startTime" -> startTime.toString(),
      "timeInMillis" -> timeInMillis,
      "purged" -> purged
    )
  }

  /**
   * Test if the requestData match the criterias given in parameter
   * @param criterias
   * @return
   */
  def checkCriterias(criterias: Criterias): Boolean = {
    checkGroup(criterias.group) &&
      checkEnv(criterias.environment) &&
      checkServiceAction(criterias.serviceAction) &&
      checkStatus(criterias.code) &&
      checkSearch(criterias.request, criterias.response, criterias.search)
  }

  /**
   * Check that the group of the requestData match the group in parameter
   * @param group
   */
  private def checkGroup(group: String): Boolean = {
    group == "all"
  }

  /**
   * Check that the environment of the requestData match the environment in parameter
   * @param environment
   */
  private def checkEnv(environment: String): Boolean = {
    environment == "all" || environment == this.environmentName
  }

  /**
   * Check that the serviceAction of the requestData match the serviceAction in parameter
   * @param serviceAction
   */
  private def checkServiceAction(serviceAction: String): Boolean = {
    serviceAction == "all" || serviceAction == this.serviceAction
  }

  /**
   * Check that the status of the RequestData match the status in parameter
   * @param status
   */
  private def checkStatus(status: String): Boolean = {
    if (status.startsWith("NOT_")) {
      val notCode = status.split("NOT_")(1)
      this.status.toString != notCode
    } else {
      status == "all" || status == this.status.toString
    }
  }

  /**
   * Check that the request or the response of the RequestData match the search query in parameter
   * @param searchRequest search in Request
   * @param searchResponse search in Response
   * @param search search in the string
   */
  private def checkSearch(searchRequest: Boolean, searchResponse: Boolean, search: String): Boolean = {
    if (searchRequest && this.request.indexOf(search) > -1) return true
    if (searchResponse && this.response.indexOf(search) > -1) return true
    // if the two checkbox are unchecked, the search field is ignore
    if (!searchRequest && !searchResponse) return true
    return false
  }
}

object RequestData {

  def collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("requestData")

  implicit val requestDataFormat = Json.format[RequestData]

  implicit object MapBSONWriter extends BSONDocumentWriter[Map[String, String]] {
    def write(m: Map[String, String]): BSONDocument = {
      val elements = m.toStream.map {
        tuple =>
          tuple._1 -> BSONString(tuple._2)
      }
      BSONDocument(elements)
    }
  }

  implicit object MapBSONReader extends BSONDocumentReader[Map[String, String]] {
    def read(bson: BSONDocument): Map[String, String] = {
      val elements = bson.elements.map {
        tuple =>
          tuple._1 -> tuple._2.toString
      }
      elements.toMap
    }
  }

  implicit object RequestDataBSONReader extends BSONDocumentReader[RequestData] {
    def read(doc: BSONDocument): RequestData = {
      RequestData(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("sender").get,
        doc.getAs[String]("serviceAction").get,
        doc.getAs[String]("environmentName").get,
        doc.getAs[List[String]]("groupsName").toList.flatten,
        doc.getAs[BSONObjectID]("serviceId").get,
        doc.getAs[String]("request").get,
        doc.getAs[Map[String, String]]("requestHeaders").get,
        doc.getAs[String]("contentType").get,
        doc.getAs[String]("requestCall"),
        new DateTime(doc.getAs[BSONDateTime]("startTime").get.value),
        doc.getAs[String]("response").get,
        doc.getAs[String]("responseOriginal"),
        doc.getAs[Map[String, String]]("responseHeaders").get,
        doc.getAs[Long]("timeInMillis").get,
        doc.getAs[Int]("status").get,
        doc.getAs[Boolean]("purged").get,
        doc.getAs[Boolean]("isMock").get
      )
    }
  }


  implicit object RequestDataBSONWriter extends BSONDocumentWriter[RequestData] {
    def write(requestData: RequestData): BSONDocument = {
      Logger.debug("requestData:" + requestData)
      BSONDocument(
        "_id" -> requestData._id,
        "sender" -> BSONString(requestData.sender),
        "serviceAction" -> BSONString(requestData.serviceAction),
        "environmentName" -> BSONString(requestData.environmentName),
        "groupsName" -> requestData.groupsName,
        "serviceId" -> requestData.serviceId,
        "request" -> BSONString(requestData.request),
        "requestHeaders" -> requestData.requestHeaders,
        "contentType" -> BSONString(requestData.contentType),
        "requestCall" -> Option(requestData.requestCall),
        "startTime" -> BSONDateTime(requestData.startTime.getMillis),
        "response" -> BSONString(requestData.response),
        "responseOriginal" -> Option(requestData.responseOriginal),
        "responseHeaders" -> requestData.responseHeaders,
        "timeInMillis" -> BSONLong(requestData.timeInMillis),
        "status" -> BSONInteger(requestData.status),
        "purged" -> BSONBoolean(requestData.purged),
        "isMock" -> BSONBoolean(requestData.isMock)
      )
    }
  }

  val keyCacheServiceAction = "serviceaction-options"
  val keyCacheStatusOptions = "status-options"
  val keyCacheMinStartTime = "minStartTime"

  /**
   * Anorm Byte conversion
   */
  //def bytes(columnName: String): RowParser[Array[Byte]] = get[Array[Byte]](columnName)(implicitly[Column[Array[Byte]]])

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "serviceAction" -> 2, "startTime" -> 3, "timeInMillis" -> 4, "environmentName" -> 5, "environmentGroups" -> 6)

  val csvKey = "requestDataStat"

  //def fetchCsv(): List[String] = DB.withConnection {
  def fetchCsv(): List[String] = {
    //TODO
    ???
  }

  /**
   * Parse required parts of a RequestData from a ResultSet in order to replay the request
   */
  /* TODO
  val forReplay = {
    get[Pk[Long]]("request_data.id") ~
      str("request_data.sender") ~
      str("request_data.serviceAction") ~
      long("request_data.environmentId") ~
      long("request_data.serviceId") ~
      bytes("request_data.request") ~
      str("request_data.requestHeaders") ~
      str("request_data.contentType") map {
      case id ~ sender ~ serviceAction ~ environnmentId ~ serviceId ~ request ~ requestHeaders ~ contentType =>
        val headers = UtilConvert.headersFromString(requestHeaders)
        new RequestData(id, sender, serviceAction, environnmentId, serviceId, uncompressString(request), headers, contentType, null, null, null, null, -1, -1, false, false)
    }
  }

  val forRESTReplay = {
    get[Pk[Long]]("request_data.id") ~
      str("request_data.sender") ~
      str("request_data.serviceAction") ~
      long("request_data.environmentId") ~
      long("request_data.serviceId") ~
      bytes("request_data.request") ~
      str("request_data.requestHeaders") ~
      str("request_data.requestCall") map {
      case id ~ sender ~ serviceAction ~ environnmentId ~ serviceId ~ request ~ requestHeaders ~ requestCall =>
        val headers = UtilConvert.headersFromString(requestHeaders)
        new RequestData(id, sender, serviceAction, environnmentId, serviceId, uncompressString(request), headers, null, requestCall, null, null, null, -1, -1, false, false)
    }
  }
  */

  /**
   * Retrieve all distinct serviceactions using name and groups
   * @return a list of serviceAction's name and groups
   */
  def serviceActionOption: Future[List[(String, List[String])]] = {
    val command =
      BSONDocument(
        "aggregate" -> collection.name, // we aggregate on collection
        "pipeline" -> BSONArray(
          BSONDocument(
            "$group" -> BSONDocument(
              "_id" -> BSONDocument("serviceAction" -> "$serviceAction", "groupsName" -> "$groupsName"),
              "nbServiceAction" -> BSONDocument("$sum" -> 1)))
        )
      )


    var listRes = ListBuffer.empty[(String, List[String])]
    val query = ReactiveMongoPlugin.db.command(RawCommand(command))
    query.map {
      b =>
        b.getAs[List[BSONValue]]("result").get.foreach {
          // For each results
          saOptions =>
            saOptions.asInstanceOf[BSONDocument].elements.foreach {
              idOption =>
                if (idOption._1 == "_id") {
                  // If the key is "_id", we retrieve the serviceAction's name and groups
                  var serviceAction = ""
                  var groups = ListBuffer.empty[String]

                  idOption._2.asInstanceOf[BSONDocument].elements.foreach {
                    s =>
                      if (s._1 == "serviceAction") {

                        serviceAction = s._2.asInstanceOf[BSONString].value
                      }
                      else {
                        s._2.asInstanceOf[BSONArray].values.foreach(e => groups += e.asInstanceOf[BSONString].value.toString)
                      }
                  }
                  listRes += ((serviceAction, groups.toList))
                }
            }
        }
        listRes.toList.sortBy(_._1)
    }
  }


  /**
   * Construct the Map[String, String] needed to fill a select options set.
   */
  def statusOptions: Future[BSONDocument] = {
    val command = RawCommand(BSONDocument("distinct" -> collection.name, "key" -> "status", "query" -> BSONDocument()))
    // example of return {"values":[200],"stats":{"n":16,"nscanned":16,"nscannedObjects":16,"timems":0,"cursor":"BasicCursor"},"ok":1.0}
    ReactiveMongoPlugin.db.command(command) // result is Future[BSONDocument]
  }

  /**
   * find the oldest requestData
   */
  def getMinRequestData: Future[Option[RequestData]] = {
    collection.find(BSONDocument()).sort(BSONDocument({
      "startTime" -> 1
    })).one[RequestData]
  }

  /**
   * Insert a new RequestData.
   * @param requestData the requestData
   */
  def insert(requestData: RequestData): Option[BSONObjectID] = {

    //TODO
    /*
    var contentRequest: String = null
    var contentResponse: String = null

    val f = Environment.findById(requestData.environmentId).map(e => e)
    val environment = Await result(f, 1.seconds)

    val service = Service.findById(requestData.serviceId).get
    val date = new Date()
    val gcal = new GregorianCalendar()
    gcal.setTime(date)
    gcal.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format

    if (!service.recordData || !environment.get.recordData) {
      Logger.debug("Data not recording for this service or this environment")
      return None
    } else if (!service.recordContentData) {
      val msg = "Content Data not recording for this service. See Admin."
      requestContent = compressString(msg)
      responseContent = compressString(msg)
      Logger.debug(msg)
    } else if (!environment.recordContentData) {
      val msg = "Content Data not recording for this environment. See Admin."
      requestContent = compressString(msg)
      responseContent = compressString(msg)
      Logger.debug(msg)
    } else if (requestData.status != 200 || (
      environment.hourRecordContentDataMin <= gcal.get(Calendar.HOUR_OF_DAY) &&
        environment.hourRecordContentDataMax > gcal.get(Calendar.HOUR_OF_DAY))) {
      // Record Data if it is a soap fault (status != 200) or
      // if we can record data with environment's configuration (hours of recording)
      requestContent = compressString(requestData.request)

      def transferEncodingResponse = requestData.responseHeaders.filter {
        _._1 == HeaderNames.CONTENT_ENCODING
      }

      transferEncodingResponse.get(HeaderNames.CONTENT_ENCODING) match {
        case Some("gzip") =>
          Logger.debug("Response in gzip Format")
          contentResponse = "TODO response in gzip format" // TODO requestData.responseBytes
        case _ =>
          Logger.debug("Response in plain Format")
          contentResponse = requestData.response
      }
    } else {
      val msg = "Content Data not recording. Record between " + environment.get.hourRecordContentDataMin + "h to " + environment.get.hourRecordContentDataMax + "h for this environment."
      contentRequest = msg
      contentResponse = msg
      Logger.debug(msg)
    }

    requestData.request = contentRequest
    requestData.response = contentResponse
*/
    def transferEncodingResponse = requestData.responseHeaders.filter {
      _._1 == HeaderNames.CONTENT_ENCODING
    }

    transferEncodingResponse.get(HeaderNames.CONTENT_ENCODING) match {
      case Some("gzip") =>
        Logger.debug("Response in gzip Format")
        requestData.responseOriginal = Some(requestData.response)
        requestData.response = uncompressString(requestData.responseBytes)
      case _ =>
        Logger.debug("Response in plain Format")
    }

    val f = collection.insert(requestData)
    f.onFailure {
      case t => new Exception("An unexpected server error has occured: " + t.getMessage)
    }
    f.map {
      lastError =>
        Logger.debug(s"Successfully inserted RequestData with LastError: $lastError")
    }
    requestData._id
  }

  /**
   * Insert a new RequestData.
   */
  def insertStats(environmentId: Long, serviceAction: String, startTime: Date, timeInMillis: Long) = {
    /*
    try {

      DB.withConnection {
        implicit connection =>

          val purgedStats = SQL(
            """
            delete from request_data
            where isStats = 'true'
            and environmentId = {environmentId}
            and startTime >= {startTime} and startTime <= {startTime}
            and serviceAction = {serviceAction}
            """
          ).on(
              'serviceAction -> serviceAction,
              'environmentId -> environmentId,
              'startTime -> startTime).executeUpdate()

          Logger.debug("Purged " + purgedStats + " existing stats for:" + environmentId + " serviceAction: " + serviceAction + " and startTime:" + startTime)

          SQL(
            """
            insert into request_data
              (sender, serviceAction, environmentId, request, requestHeaders, startTime, response, responseHeaders, timeInMillis, status, isStats, isMock) values (
              '', {serviceAction}, {environmentId}, '', '', {startTime}, '', '', {timeInMillis}, 200, 'true', 'false'
            )
            """).on(
              'serviceAction -> serviceAction,
              'environmentId -> environmentId,
              'startTime -> startTime,
              'timeInMillis -> timeInMillis).executeUpdate()
      }
    } catch {
      case e: Exception => Logger.error("Error during insertion of RequestData Stats", e)
    }
    */
  }

  /**
   * Delete data (request & reponse) between min and max date
   * @param environmentIn environmement or "" / all if all
   * @param minDate min date
   * @param maxDate max date
   * @param user use who delete the data : admin or akka
   */
  def deleteRequestResponse(environmentIn: String, minDate: Date, maxDate: Date, user: String): Int = {
    val minDateTime = new DateTime(minDate)
    val maxDateTime = new DateTime(maxDate)

    val selector = environmentIn match {
      case "all" =>
        // Modify the request data between the two datetime from all environment
        BSONDocument(
          "startTime" -> BSONDocument(
            "$gte" -> BSONDateTime(minDateTime.getMillis),
            "$lt" -> BSONDateTime(maxDateTime.getMillis)),
          "purged" -> false,
          "isStats" -> null
        )

      case _ =>
        BSONDocument(
          "environmentName" -> environmentIn,
          "startTime" -> BSONDocument(
            "$gte" -> BSONDateTime(minDateTime.getMillis),
            "$lt" -> BSONDateTime(maxDateTime.getMillis)),
          "purged" -> false,
          "isStats" -> null
        )
    }

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "response" -> "",
        "responseOriginal" -> "",
        "request" -> "",
        "requestHeaders" -> "",
        "responseHeaders" -> "",
        "purged" -> true
      ))

    Cache.remove(keyCacheServiceAction)
    Cache.remove(keyCacheStatusOptions)

    var updatedElement = 0
    val futureUpdate = collection.update(selector, modifier, multi = true)

    futureUpdate.onComplete {
      case Failure(e) => throw e

      case Success(lastError) => {
        if (lastError.updatedExisting) {
          updatedElement = lastError.updated
          Logger.debug(updatedElement + " RequestData of the environment " + environmentIn + " has been purged by " + user)
        }
      }
    }
    return updatedElement
  }

  /**
   * Delete entries between min and max date
   * @param environmentIn environmement or "" / all if all
   * @param minDate min date
   * @param maxDate max date
   */
  def delete(environmentIn: String, minDate: Date, maxDate: Date): Int = {

    val minDateTime = new DateTime(minDate)
    val maxDateTime = new DateTime(maxDate)

    val selector = environmentIn match {
      case "all" =>
        // Remove the request data between the two datetime from all environment
        BSONDocument(
          "startTime" -> BSONDocument(
            "$gte" -> BSONDateTime(minDateTime.getMillis),
            "$lt" -> BSONDateTime(maxDateTime.getMillis)),
          "isStats" -> null
        )

      case _ =>
        BSONDocument(
          "environmentName" -> environmentIn,
          "startTime" -> BSONDocument(
            "$gte" -> BSONDateTime(minDateTime.getMillis),
            "$lt" -> BSONDateTime(maxDateTime.getMillis)),
          "isStats" -> null
        )
    }
    var removedElement = 0
    val futurRemove = collection.remove(selector)

    futurRemove.onComplete {
      case Failure(e) => throw e

      case Success(lastError) =>
        removedElement = lastError.updated
        Logger.debug(removedElement + " RequestData of the environment " + environmentIn + " has been purged")
    }
    Cache.remove(keyCacheServiceAction)
    Cache.remove(keyCacheStatusOptions)

    return removedElement
  }

  /**
   * Return a page of RequestData
   * @param groups groups name
   * @param environmentIn name of environnement, "all" default
   * @param serviceAction serviceAction, "all" default
   * @param minDate Min Date
   * @param maxDate Max Date
   * @param status Status
   * @param offset offset in search
   * @param pageSize size of line in one page
   * @return
   */
  def list(groups: String, environmentIn: String, serviceAction: String, minDate: Date, maxDate: Date, status: String, offset: Int = 0,
           pageSize: Int = 10, sSearch: String, request: Boolean, response: Boolean): Future[List[RequestData]] = {

    var query = BSONDocument()
    if (environmentIn == "all") {
      // We retrieve the environments of the groups in parameter
      val environments = Environment.optionsInGroups(groups)
      // We add the environments names to the query
      query = query ++ ("environmentName" -> BSONDocument("$in" -> environments.map {
        e => e._2
      }.toArray))
    } else {
      query = query ++ ("environmentName" -> environmentIn)
    }

    if (serviceAction != "all") query = query ++ ("serviceAction" -> serviceAction)

    query = query ++ ("startTime" -> BSONDocument(
      "$gte" -> BSONDateTime(minDate.getTime),
      "$lt" -> BSONDateTime(maxDate.getTime))
      )

    if (status != "all") {
      if (status.startsWith("NOT_")) {
        val notCode = status.split("NOT_")(1)
        query = query ++ ("status" -> BSONDocument("$ne" -> notCode.toInt))
      }
      else query = query ++ ("status" -> status.toInt)
    }

    if (sSearch != "") {
      // We use regex research instead of mongoDb $text
      if (request && response) query = query ++ ("$or" -> BSONArray(BSONDocument("request" -> BSONDocument("$regex" -> sSearch)), BSONDocument("response" -> BSONDocument("$regex" -> sSearch))))
      else if (request) query = query ++ ("request" -> BSONDocument("$regex" -> sSearch))
      else if (response) query = query ++ ("response" -> BSONDocument("$regex" -> sSearch))
    }

    collection.
      find(query).
      sort(BSONDocument("startTime" -> -1)).
      cursor[RequestData].
      collect[List]()
  }

  /**
   * Load reponse times for given parameters
   */
  def findResponseTimes(groupName: String, environmentIn: String, serviceAction: String, minDate: Date, maxDate: Date, status: String, statsOnly: Boolean): List[(Long, String, Date, Long)] = {
    ???
    /*
      var whereClause = "where startTime >= {minDate} and startTime <= {maxDate}"
      whereClause += sqlAndStatus(status)
      Logger.debug("DDDDD==>" + whereClause)
      if (serviceAction != "all") whereClause += " and serviceAction = {serviceAction}"
      whereClause += sqlAndEnvironnement(environmentIn)

      val pair = sqlFromAndGroup(groupName, environmentIn)
      val fromClause = " from request_data " + pair._1
      whereClause += pair._2

      val params: Array[(Any, anorm.ParameterValue[_])] = Array(
        'minDate -> minDate,
        'maxDate -> maxDate,
        'serviceAction -> serviceAction)

      var sql = "select environmentId, serviceAction, startTime, timeInMillis " + fromClause + whereClause

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
            .as(get[Long]("environmentId") ~ get[String]("serviceAction") ~ get[Date]("startTime") ~ get[Long]("timeInMillis") *)
            .map(flatten)
      }*/

  }


  def loadAvgResponseTimesByEnvirAndAction(groupName: String, environmentName: String, serviceAction: String, status: String, minDate: Date, maxDate: Date, withStats: Boolean): Long = {
    ???
    /*
    var avg = -1.toLong
    DB.withConnection {
      implicit connection =>
        var whereClause = " where startTime >= {minDate} and startTime <= {maxDate} "
        if (!withStats) whereClause += " and isStats = 'false' "
        whereClause += " and serviceAction = \"" + serviceAction + "\""

        if (groupName != "all")
          whereClause += "and request_data.id = environment.id and environment.groupId = " + Group.options.find(t => t._2 == groupName).get._1
        else
          whereClause += "and request_data.environmentId = environment.id "

        whereClause += "and environment.name = \"" + environmentName + "\" "
        val fromClause = " from request_data, environment, groups "

        val sql = "select timeInMillis " + fromClause + whereClause + " order by timeInMillis asc"
        Logger.debug("SQL (loadAvgResponseTimesByEnvirAndAction) ====> " + sql)
        val responseTimes = SQL(sql)
          .on(
            'minDate -> minDate,
            'maxDate -> maxDate)
          .as(get[Long]("timeInMillis") *)
          .toList
        val times = responseTimes.slice(0, responseTimes.size * 9 / 10)
        if (responseTimes.size != 0 && times.size != 0) {
          avg = times.sum / times.size
        }
    }
    avg
    */
  }

  def loadAvgResponseTimesBySpecificAction(groupName: String, serviceAction: String, status: String, minDate: Date, maxDate: Date, withStats: Boolean): Long = {
    ???
    /*
    var avg = -1.toLong
    DB.withConnection {
      implicit connection =>
        var whereClause = " where startTime >= {minDate} and startTime <= {maxDate} "
        whereClause += sqlAndStatus(status)
        if (!withStats) whereClause += " and isStats = 'false' "
        Logger.debug("Load Stats with ServiceAction: " + serviceAction)
        whereClause += " and serviceAction = \"" + serviceAction + "\""

        if (groupName != "all")
          whereClause += "and request_data.id = environment.id and environment.groupId = " + Group.options.find(t => t._2 == groupName).get._1
        else
          whereClause += "and request_data.environmentId = environment.id"

        val fromClause = " from request_data, environment, groups "
        val sql = "select timeInMillis " + fromClause + whereClause + " order by timeInMillis asc"
        Logger.debug("SQL (loadAvgResponseTimesBySpecificAction) ====> " + sql)

        val responseTimes = SQL(sql)
          .on(
            'minDate -> minDate,
            'maxDate -> maxDate)
          .as(get[Long]("timeInMillis") *)
          .toList



        val times = responseTimes.slice(0, responseTimes.size * 9 / 10)
        if (responseTimes.size != 0 && times.size != 0) {
          avg = times.sum / times.size;
        }
    }
    avg
    */
  }

  def loadAvgResponseTimesByAction(groupName: String, environmentName: String, status: String, minDate: Date, maxDate: Date, withStats: Boolean): List[(String, Long)] = {
    ???
    /*DB.withConnection {
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

        val sql = "select serviceAction, timeInMillis " + fromClause + whereClause + " order by timeInMillis asc "

        Logger.debug("SQL (loadAvgResponseTimesByAction) ====> " + sql)

        val responseTimes = SQL(sql)
          .on(
            'minDate -> minDate,
            'maxDate -> maxDate)
          .as(get[String]("serviceAction") ~ get[Long]("timeInMillis") *)
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
    }*/
  }

  /**
   * Find all day before today, for environment given and state = 200.
   * @param environmentId environment id
   * @return list of unique date
   */
  def findDayNotCompileStats(environmentId: String): List[Date] = {
    ???
    /*
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
    */
  }

  def load(id: Long): RequestData = {
    ???
    /*DB.withConnection {
      implicit connection =>
        SQL("select id, sender, serviceAction, environmentId, serviceId, request, requestHeaders, contentType, requestCall from request_data where id= {id}")
          .on('id -> id).as(RequestData.forReplay.single)
    }*/
  }

  def loadRequest(id: String): Future[Option[BSONDocument]] = {
    val query = BSONDocument("_id" -> BSONObjectID(id))
    val projection = BSONDocument("request" -> 1, "contentType" -> 1)
    collection.find(query, projection).cursor[BSONDocument].headOption
  }

  def loadResponse(id: String): Future[Option[BSONDocument]] = {
    val query = BSONDocument("_id" -> BSONObjectID(id))
    val projection = BSONDocument("response" -> 1, "contentType" -> 1)
    collection.find(query, projection).cursor[BSONDocument].headOption
  }

  /**
   * Upload a csvLine => insert requestDataStat.
   *
   * @param csvLine line in csv file
   * @return nothing
   */
  def upload(csvLine: String) = {
    ???
    /*
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
          insertStats(environment.id, dataCsv(csvTitle.get("serviceAction").get).trim, UtilDate.parse(dataCsv(csvTitle.get("startTime").get).trim), dataCsv(csvTitle.get("timeInMillis").get).toLong)
      }.getOrElse {
        Logger.warn("Warning : Environment " + environmentName + " unknown")
        throw new Exception("Warning : Environment " + environmentName + " already exist")
      }
    }
    */
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
