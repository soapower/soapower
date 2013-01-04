package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.UtilDate._
import play.api.libs.iteratee.Enumerator
import play.api.http.HeaderNames
import scala.xml.PrettyPrinter
import org.xml.sax.SAXParseException
import java.util.Date

case class Search(environmentId: Long)

object Search extends Controller {

  def index(environment: String, soapAction: String, minDate: String, maxDate: String, status: String) = Action {
    implicit request =>
      Ok(views.html.search.index(environment, soapAction, formatDate(getDate(minDate)), formatDate(getDate(maxDate)), status, Environment.options, RequestData.soapActionOptions, RequestData.statusOptions))
  }

  def listDatatable(environment: String, soapAction: String, minDate: String, maxDate: String, status: String, sSearch: String, iDisplayStart: Int, iDisplayLength: Int) = Action {
    val page: Page[(RequestData)] = RequestData.list(environment, soapAction, getDate(minDate).getTime, getDate(maxDate, v23h59min59s).getTime, status, iDisplayStart, iDisplayLength, sSearch)

    Ok(Json.toJson(Map(
      "iTotalRecords" -> Json.toJson(iDisplayLength),
      "iTotalDisplayRecords" -> Json.toJson(page.total),
      "aaData" -> Json.toJson(page.items)))).as(JSON)
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
          body = Enumerator(formattedXml))
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
          body = Enumerator(formattedXml))
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
