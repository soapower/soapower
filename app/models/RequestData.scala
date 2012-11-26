package models

import java.util.{ Date }

import play.api.db._
import play.api.Play.current
import play.api._

import anorm._
import anorm.SqlParser._

case class RequestData(
  id: Pk[Long],
  localTarget: String,
  remoteTarget: String,
  request: String,
  startTime: Date,
  var response: String,
  var timeInMillis: Long,
  var status: Int) {

  def this(localTarget: String, remoteTarget: String, request: String) =
    this(null, localTarget, remoteTarget, request, new Date, null, -1, -1)

}

object RequestData {

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
              (id, localTarget, remoteTarget, request, startTime, response, timeInMillis, status) values (
              (select next value for request_data_seq), 
              {localTarget}, {remoteTarget}, {request}, {startTime}, {response}, {timeInMillis}, {status}
            )
          """).on(
            'localTarget -> requestData.localTarget,
            'remoteTarget -> requestData.remoteTarget,
            'request -> requestData.request,
            'startTime -> requestData.startTime,
            'response -> requestData.response,
            'timeInMillis -> requestData.timeInMillis,
            'status -> requestData.status).executeUpdate()
      }

    } catch {
      //case e:SQLException => Logger.error("Database error")
      //case e:MalformedURLException => Logger.error("Bad URL")
      case e: Exception => Logger.error("Caught an exception! ", e)
    }
  }

}
