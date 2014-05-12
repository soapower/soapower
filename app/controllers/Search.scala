package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.UtilDate._
import play.api.libs.iteratee.Enumerator
import play.api.http.{ContentTypes, HeaderNames}
import scala.xml.PrettyPrinter
import org.xml.sax.SAXParseException
import java.net.URLDecoder
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import play.api.Logger
import reactivemongo.bson.{BSONObjectID, BSONDocument}

case class Search(environmentId: Long)

object Search extends Controller {

  private val UTF8 = "UTF-8"

  def listDatatable(groups: String, environment: String, serviceAction: String, minDate: String, maxDate: String, status: String, sSearch: String, iDisplayStart: Int, iDisplayLength: Int, request: Boolean, response: Boolean) = Action.async {

    val futureDataList = RequestData.list(groups, environment, URLDecoder.decode(serviceAction, UTF8), getDate(minDate).getTime, getDate(maxDate, v23h59min59s, true).getTime, status, (iDisplayStart - 1), iDisplayLength, sSearch, request, response)
    futureDataList.map {
      list =>
        Ok(Json.toJson(Map("data" -> Json.toJson(list))))
    }
  }

  /**
   * Used to download or render the request
   * @param id of the requestData to download
   * @param asFile "true" or "false"
   * @return
   */
  def downloadRequest(id: String, asFile: String) = Action.async {
    val future = RequestData.loadRequest(id)
    downloadInCorrectFormat(future, id, ("true".equals(asFile)), true)
  }

  /**
   * Used to download or render the response
   * @param id of the requestData to download
   * @param asFile "true" or "false"
   * @return
   */
  def downloadResponse(id: String, asFile: String) = Action.async {
    val future = RequestData.loadResponse(id)
    downloadInCorrectFormat(future, id, ("true".equals(asFile)), false)
  }


  /**
   * Download the response / request in the correct format
   * @param future the request or response Content
   * @param asFile
   * @param isRequest
   * @return
   */
  //def downloadInCorrectFormat(str: String, id: String, format: String, asFile: Boolean, isRequest: Boolean) = {
  def downloadInCorrectFormat(future: Future[Option[BSONDocument]], id: String, asFile: Boolean, isRequest: Boolean) = {

    val keyContent = {
      if (isRequest) "request" else "response"
    }
    var filename = ""
    if (isRequest) {
      filename = "request-" + id
    }
    else {
      filename = "response-" + id
    }

    var contentInCorrectFormat = ""

    future.map {
      tuple => tuple match {
        case Some(doc: BSONDocument) => {
          val contentType = tuple.get.getAs[String]("contentType").get
          val content = tuple.get.getAs[String](keyContent).get

          contentType match {
            case "application/xml" | "text/xml" => {
              try {
                contentInCorrectFormat = new PrettyPrinter(250, 4).format(scala.xml.XML.loadString(content))
                filename += ".xml"
              } catch {
                case e: SAXParseException => contentInCorrectFormat = content
                  filename += ".txt"
              }

              var result = SimpleResult(
                header = ResponseHeader(play.api.http.Status.OK),
                body = Enumerator(contentInCorrectFormat.getBytes))
              if (asFile) {
                result = result.withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename))
                result.as(XML)
              } else {
                result.as(TEXT)
              }
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
              var result = SimpleResult(
                header = ResponseHeader(play.api.http.Status.OK),
                body = Enumerator(contentInCorrectFormat.getBytes))
              if (asFile) {
                result = result.withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename))
                result.as(JSON)
              } else {
                result.as(TEXT)
              }
            }
            case _ => {
              filename += ".txt"
              var result = SimpleResult(
                header = ResponseHeader(play.api.http.Status.OK),
                body = Enumerator(content.getBytes))
              if (asFile) {
                result = result.withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename))
              }
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

