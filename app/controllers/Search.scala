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

  def index = Action {
    Ok(views.html.search.index())
  }

  def listDatatable(sSearch:String, iDisplayStart: Int, iDisplayLength: Int) = Action { 
  	// TODO add RequestData.findSearchAllOrder(sSearch, iDisplayStart, iDisplayLength)
    Ok(Json.toJson( Map(
      "iTotalRecords" -> Json.toJson(100),
      "iTotalDisplayRecords" -> Json.toJson(10),
      "aaData" -> Json.toJson(RequestData.all)
    ))).as(JSON)
	}

}
