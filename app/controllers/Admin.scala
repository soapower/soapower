package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._


object Admin extends Controller {

  val Home = Redirect(routes.Admin.index())


  def index = Action { implicit request =>
      Ok(views.html.admin.index())
  }

  def deleteAllRequestData = Action {
    RequestData.deleteAll()
    Home.flashing("success" -> "Request Data deleted")
  }
}
