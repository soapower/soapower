package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.UtilDate._
import play.api.libs.iteratee.Enumerator
import play.api.http.HeaderNames
import scala.xml.PrettyPrinter
import org.xml.sax.SAXParseException
import java.net.URLDecoder
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

case class Search(environmentId: Long)

object Search extends Controller {

  private val UTF8 = "UTF-8"

  def listDatatable(group: String, environment: String, soapAction: String, minDate: String, maxDate: String, status: String, iDisplayStart: Int, iDisplayLength: Int) = Action.async {
    val futureDataList = RequestData.list(group, environment, URLDecoder.decode(soapAction, UTF8), getDate(minDate).getTime, getDate(maxDate, v23h59min59s, true).getTime, status, (iDisplayStart-1), iDisplayLength)

    futureDataList.map { list =>
      Ok(Json.toJson(Map("data" -> Json.toJson(list))))
    }
  }

  def downloadRequest(id: Long, asFile: Boolean) = Action {
    val request = RequestData.loadRequest(id)

    request match {
      case Some(str: String) => {
        var formattedXml = ""
        try {
          formattedXml = new PrettyPrinter(250, 4).format(scala.xml.XML.loadString(str))
        } catch {
          case e:SAXParseException => formattedXml = str
        }
        val filename = "request-" + id + ".xml"

        var result = SimpleResult(
          header = ResponseHeader(play.api.http.Status.OK),
          body = Enumerator(formattedXml.getBytes))
        if (asFile) {
          result = result.withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename))
          result.as(XML)
        } else {
          result.as(TEXT)
        }
      }
      case _ => NotFound("The request does not exist")
    }

  }

  def downloadResponse(id: Long, asFile: Boolean) = Action {
    val response = RequestData.loadResponse(id)

    response match {
      case Some(str: String) => {
        var formattedXml = ""
        try {
          formattedXml = new PrettyPrinter(250, 4).format(scala.xml.XML.loadString(str))
        } catch {
          case e:SAXParseException => formattedXml = str
        }

        val filename = "response-" + id + ".xml"
        var result = SimpleResult(
          header = ResponseHeader(play.api.http.Status.OK),
          body = Enumerator(formattedXml.getBytes))
        if (asFile) {
          result = result.withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename))
          result.as(XML)
        } else {
          result.as(TEXT)
        }
      }

      case _ => NotFound("The response does not exist")
    }
  }

}
