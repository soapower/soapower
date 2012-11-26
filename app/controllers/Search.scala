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

    Logger.debug("iDisplayStart, iDisplayLength" + iDisplayStart + ", " + iDisplayLength)

    val page : Page[(RequestData)] = RequestData.list(iDisplayStart - 1, iDisplayLength, sSearch)

    Ok(Json.toJson( Map(
      "iTotalRecords" -> Json.toJson(iDisplayLength),
      "iTotalDisplayRecords" -> Json.toJson(page.total),
      "aaData" -> Json.toJson(page.items)
    ))).as(JSON)
	}

}
