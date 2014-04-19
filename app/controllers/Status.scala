package controllers

import play.api.mvc._

import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import play.modules.reactivemongo.json.BSONFormats._


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
  def findAll = Action.async {
    RequestData.statusOptions.map {
      s =>
        Ok(Json.toJson(s)).as(JSON)
    }
  }

}
