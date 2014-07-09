package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.UtilDate._
import play.api.libs.iteratee.Enumerator
import play.api.http.{HeaderNames}
import scala.xml.PrettyPrinter
import org.xml.sax.SAXParseException
import java.net.URLDecoder
import scala.concurrent.{Await, Future, ExecutionContext}
import ExecutionContext.Implicits.global
import reactivemongo.bson.BSONDocument
import scala.util.parsing.json.JSONObject
import scala.concurrent.duration._
import com.fasterxml.jackson.core.JsonParseException

case class Search(environmentId: Long)

object Search extends Controller {

  private val UTF8 = "UTF-8"

  def listDatatable(groups: String, environment: String, serviceAction: String, minDate: String, maxDate: String, status: String, sSearch: String, page: Int, pageSize: Int, sortKey: String, sortVal: String, request: Boolean, response: Boolean) = Action.async {
    val futureDataList = RequestData.list(groups, environment, URLDecoder.decode(serviceAction, UTF8), getDate(minDate).getTime, getDate(maxDate, v23h59min59s, true).getTime, status, (page - 1), pageSize, sortKey, sortVal, sSearch, request, response)
    val futureTotalSize = RequestData.getTotalSize(groups, environment, URLDecoder.decode(serviceAction, UTF8), getDate(minDate).getTime, getDate(maxDate, v23h59min59s, true).getTime, status, sSearch, request, response)

    for {
      futureDataListResult <- futureDataList
      futureTotalSizeResult <- futureTotalSize
    } yield (Ok(Json.toJson(Map("data" -> Json.toJson(futureDataListResult),
      "totalDataSize" -> Json.toJson(futureTotalSizeResult.asInstanceOf[Long])))))
  }

  /**
   * Used to download or render the request
   * @param id of the requestData to download
   * @return
   */
  def downloadRequest(id: String) = Action.async {
    val future = RequestData.loadRequest(id)
    downloadInCorrectFormat(future, id, true)
  }

  /**
   * Get the requestData request content and send it to the client
   * @param id
   * @return
   */
  def getRequest(id: String) = Action.async {
    RequestData.loadRequest(id).map {
      tuple => tuple match {
        case Some(doc: BSONDocument) => {
          doc.getAs[String]("contentType").get match {
            case "application/json" =>
              val content = doc.getAs[String]("request").get
              try {
                Ok(Json.toJson(Json.parse(doc.getAs[String]("request").get)));
              }
              catch {
                case e: JsonParseException =>
                  Ok(content)
              }
            case "application/xml" | "text/xml" => {
              var content = doc.getAs[String]("request").get
              try {
                content = new PrettyPrinter(250, 4).format(scala.xml.XML.loadString(content))
              } catch {
                case e: SAXParseException =>
              }
              Ok(content)
            }
            case _ =>
              Ok(doc.getAs[String]("request").get)
          }
        }
        case None =>
          NotFound("The request does not exist")
      }
    }
  }

  /**
   * Get the response requestData and send it to the client
   * @param id
   * @return
   */
  def getResponse(id: String) = Action.async {
    RequestData.loadResponse(id).map {
      tuple => tuple match {
        case Some(doc: BSONDocument) => {
          doc.getAs[String]("contentType").get match {
            case "application/json" =>
              var content = doc.getAs[String]("response").get
              try {
                val content = Json.parse(doc.getAs[String]("response").get)
                Ok(Json.toJson(content));
              }
              catch {
                case e: JsonParseException =>
                  Ok(content)
              }
            case "application/xml" | "text/xml" => {
              var content = doc.getAs[String]("response").get
              try {
                content = new PrettyPrinter(250, 4).format(scala.xml.XML.loadString(content))
              } catch {
                case e: SAXParseException =>

              }
              Ok(content)
            }
            case _ =>
              Ok(doc.getAs[String]("response").get)
          }
        }
        case None =>
          NotFound("The response does not exist")
      }
    }
  }

  /**
   * Used to download or render the response
   * @param id of the requestData to download
   * @return
   */
  def downloadResponse(id: String) = Action.async {
    val future = RequestData.loadResponse(id)
    downloadInCorrectFormat(future, id, false)
  }


  /**
   * Download the response / request in the correct format
   * @param future the request or response Content
   * @param isRequest
   * @return
   */
  def downloadInCorrectFormat(future: Future[Option[BSONDocument]], id: String, isRequest: Boolean) = {

    val keyContent = {
      if (isRequest) "request" else "response"
    }
    var filename = ""
    if (isRequest) {
      filename = "request-" + id
    } else {
      filename = "response-" + id
    }

    var contentInCorrectFormat = ""

    future.map {
      tuple => tuple match {
        case Some(doc: BSONDocument) => {
          val contentType = doc.getAs[String]("contentType").get
          // doc.getAs[String]("response")
          val content = doc.getAs[String](keyContent).get

          contentType match {
            case "application/xml" | "text/xml" => {
              try {
                contentInCorrectFormat = new PrettyPrinter(250, 4).format(scala.xml.XML.loadString(content))
                filename += ".xml"
              } catch {
                case e: SAXParseException => contentInCorrectFormat = content
                  filename += ".txt"
              }

              var result = Result(
                header = ResponseHeader(play.api.http.Status.OK),
                body = Enumerator(contentInCorrectFormat.getBytes))

                result = result.withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename))
                result.as(XML)

            }

            case "application/json" => {
              try {
                contentInCorrectFormat = Json.parse(content).toString
                filename += ".json"
              }
              catch {
                case e: Exception =>
                  contentInCorrectFormat = content
                  filename += ".txt"
              }
              var result = Result(
                header = ResponseHeader(play.api.http.Status.OK),
                body = Enumerator(contentInCorrectFormat.getBytes))
                result = result.withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename))
                result.as(JSON)
            }
            case _ => {
              filename += ".txt"
              var result = Result(
                header = ResponseHeader(play.api.http.Status.OK),
                body = Enumerator(content.getBytes))
                result = result.withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename))
                result.as(TEXT)
            }

          }
        }
        case _ =>
          NotFound("The request does not exist")
      }
    }

  }
}
