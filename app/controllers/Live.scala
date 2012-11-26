package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._

object Live extends Controller {

  def index = Action {
    Ok(views.html.live.index())
  }

}
