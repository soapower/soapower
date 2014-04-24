package controllers

import play.api.mvc._
import play.api.libs.json.JsValue
import models.LiveRoom

object Live extends Controller {

  /**
   * Handles the websocket.
   */
  def socket() = WebSocket.async[JsValue] {
    implicit request =>
      LiveRoom.join(request.remoteAddress)
  }

}
