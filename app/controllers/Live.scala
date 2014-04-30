package controllers

import play.api.mvc._
import play.api.libs.json.{Json, JsValue}
import models.{Criterias, LiveRoom}
import play.Logger

object Live extends Controller {

  case class Form(search: String, request: Boolean, response: Boolean)

  /**
   * Handles the websocket.
   */
  def socket() = WebSocket.async[JsValue] {
    implicit request =>
      LiveRoom.join(request.remoteAddress)
  }

  implicit val format = Json.format[Form]

  /**
   * Change the criteria of a user in the LiveRoom
   * @return
   */
  def changeCriteria() = Action(parse.json) {
    implicit request =>
      request.body.validate(format).map {
        form =>
          Logger.debug(form.search)
          Logger.debug(form.request.toString)
          Logger.debug(form.response.toString)
          val criterias = new Criterias("all", "all", "all", form.search, form.request, form.response)
          LiveRoom.changeCriterias(request.remoteAddress, criterias)
          Ok("Criterias changed")
      }.getOrElse {
        BadRequest("Wrong criterias format")
      }
  }
}
