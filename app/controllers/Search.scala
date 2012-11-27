package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._

import models._
import anorm._
import java.util.{ Date }

object Search extends Controller {

  def index(environment: String, soapAction: String) = Action {
    Ok(views.html.search.index())
  }

  def listDatatable(environment: String, soapAction: String, sSearch:String, iDisplayStart: Int, iDisplayLength: Int) = Action { 
    val page : Page[(RequestData)] = RequestData.list(environment, soapAction, iDisplayStart, iDisplayLength, sSearch)
    Ok(Json.toJson( Map(
      "iTotalRecords" -> Json.toJson(iDisplayLength),
      "iTotalDisplayRecords" -> Json.toJson(page.total),
      "aaData" -> Json.toJson(page.items)
    ))).as(JSON)
	}

}
