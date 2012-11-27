package controllers

import play.api.mvc._

object Live extends Controller {

  def index = Action {
    Ok(views.html.live.index())
  }

}
