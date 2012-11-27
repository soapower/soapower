package controllers

import play.api.mvc._
import play.api.libs.json._

import models._

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
      "aaData" -> Json.toJson(page.items)
    ))).as(JSON)
  }

}
