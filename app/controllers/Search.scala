package controllers

import play.api._
import play.api.mvc._
import play.api.data.Forms._
import play.api.data._

object Search extends Controller {

  def index = Action {
    Ok(views.html.search.index())
  }

}
