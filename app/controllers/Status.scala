package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import anorm._

import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

object Status extends Controller {

  private implicit object StatusWrites extends Writes[(Int, Int)] {
    def writes(data: (Int, Int)): JsValue = {
      JsObject(
        List(
          "id" -> JsNumber(data._1),
          "name" -> JsNumber(data._2)
        ))
    }
  }

  /**
   * Return all status in Json Format
   * @return JSON
   */
  def findAll = Action {
    Ok(Json.toJson(RequestData.statusOptions)).as(JSON)
  }

}
