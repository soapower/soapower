package controllers

import play.api.mvc._
import play.api.libs.json.JsValue
import models.LiveRoom
import play.Logger

object Live extends Controller {

  /**
   * Handles the websocket.
   */
  def socket() = WebSocket.async[JsValue] { implicit request  =>
    Logger.info("headers:" + request.remoteAddress)
    LiveRoom.join(request.remoteAddress)
  }

}
