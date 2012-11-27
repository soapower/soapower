package models

import java.util.{ Date }

import play.api.db._
import play.api.Play.current
import play.api._
import play.api.libs.json._

import anorm._
import anorm.SqlParser._

case class RequestData(
  id: Pk[Long],
  sender: String,
  environmentId: Long,
  localTarget: String,
  remoteTarget: String,
  request: String,
  startTime: Date,
  var response: String,
  var timeInMillis: Long,
  var status: Int) {

  def this(sender: String, environnmentId: Long, localTarget: String, remoteTarget: String, request: String) =
    this(null, sender, environnmentId, localTarget, remoteTarget, request, new Date, null, -1, -1)

}

object RequestData {

  /**
   * Parse a RequestData from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("request_data.id") ~
      str("request_data.sender") ~
      long("request_data.environnmentId") ~
      str("request_data.localTarget") ~
      str("request_data.remoteTarget") ~
      get[Date]("request_data.startTime") ~
      long("request_data.timeInMillis") ~
      int("request_data.status") map {
        case id ~ sender ~ environnmentId ~ localTarget ~ remoteTarget ~ startTime ~ timeInMillis ~ status =>
          RequestData(id, sender, environnmentId, localTarget, remoteTarget, null, startTime, null, timeInMillis, status)
      }
  }

  /**
   * Insert a new RequestData.
   *
   * @param requestData the requestData
   */
  def insert(requestData: RequestData) = {
    try {
      DB.withConnection { implicit connection =>
        SQL(
          """
            insert into request_data 
              (id, sender, environmentId, localTarget, remoteTarget, request, startTime, response, timeInMillis, status) values (
              (select next value for request_data_seq), 
              {sender}, {environmentId}, {localTarget}, {remoteTarget}, {request}, {startTime}, {response}, {timeInMillis}, {status}
            )
          """).on(
            'sender -> requestData.sender,
            'environmentId -> requestData.environmentId,
            'localTarget -> requestData.localTarget,
            'remoteTarget -> requestData.remoteTarget,
            'request -> requestData.request,
            'startTime -> requestData.startTime,
            'response -> requestData.response,
            'timeInMillis -> requestData.timeInMillis,
            'status -> requestData.status).executeUpdate()
      }

    } catch {
      case e: Exception => Logger.error("Error during insertion of RequestData ", e)
    }
  }

  /*
  * Get All RequestData, used for testing only
  *
  */
  def findAll(): List[RequestData] = DB.withConnection { implicit c =>
    SQL("select * from request_data").as(RequestData.simple *)
  }

  /**
   * Return a page of (RequestData).
   *
   * @param page Page to display
   * @param pageSize Number of requestData per page
   * @param orderBy RequestData property used for sorting
   * @param filter Filter applied on the name column
   */
  def list(offset: Int = 0, pageSize: Int = 10, filterIn: String = "%"): Page[(RequestData)] = {

    var filter = "%" + filterIn + "%"

    DB.withConnection { implicit connection =>

      val requests = SQL(
        """
          select id, localTarget, remoteTarget, startTime, timeInMillis, status from request_data
          where request_data.remoteTarget like {filter}
          order by request_data.id desc
          limit {pageSize} offset {offset}
        """).on(
          'pageSize -> pageSize,
          'offset -> offset,
          'filter -> filter).as(RequestData.simple *)

      val totalRows = SQL(
        """
          select count(id) from request_data 
          where request_data.remoteTarget like {filter}
        """).on(
          'filter -> filter).as(scalar[Long].single)
      Page(requests, -1, offset, totalRows)
    }
  }

  // use by Json : from scala to json
  implicit object RequestDataWrites extends Writes[RequestData] {

    def writes(o: RequestData): JsValue = JsObject(
      List("0" -> JsString(o.id.toString),
        "1" -> JsString(o.localTarget),
        "2" -> JsString(o.remoteTarget),
        "3" -> JsString("request file"),
        "4" -> JsString(o.startTime.toString),
        "5" -> JsString("reponse file"),
        "6" -> JsString(o.timeInMillis.toString),
        "7" -> JsString(o.status.toString)))
  }

}
