package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import play.api.libs.iteratee.Enumerator

case class Search(environmentId: Long)

object Search extends Controller {

  def index(environment: String, soapAction: String) = Action {
    implicit request =>
      Ok(views.html.search.index(environment, soapAction, Environment.options, RequestData.soapActionOptions))
  }

  def listDatatable(environment: String, soapAction: String, sSearch: String, iDisplayStart: Int, iDisplayLength: Int) = Action {
    val page: Page[(RequestData)] = RequestData.list(environment, soapAction, iDisplayStart, iDisplayLength, sSearch)

    Ok(Json.toJson(Map(
      "iTotalRecords" -> Json.toJson(iDisplayLength),
      "iTotalDisplayRecords" -> Json.toJson(page.total),
      "aaData" -> Json.toJson(page.items)))).as(JSON)
  }

  def downloadRequest(id: Long) = Action {
    val request = RequestData.loadRequest(id)
    val fileContent: Enumerator[String] = Enumerator(request)
    val filename = "request-" + id + ".xml"

    SimpleResult(
      header = ResponseHeader(200),
      body = fileContent)
      .withHeaders(("Content-Disposition", "attachment; filename=" + filename), ("Content-Type", "text/xml"))
  }

  def downloadResponse(id: Long) = Action {
    val response = RequestData.loadResponse(id)

    response match {
      case Some(str: String) => {
        val fileContent: Enumerator[String] = Enumerator(str)
        val filename = "response-" + id + ".xml"
        SimpleResult(
          header = ResponseHeader(200),
          body = fileContent)
          .withHeaders(("Content-Disposition", "attachment; filename=" + filename), ("Content-Type", "text/xml"))
      }

      case _ => NotFound("The response does not exist")
    }
  }

}
