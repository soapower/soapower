package controllers

import play.api.mvc._
import play.api.libs.json._

import models._
import java.util.Date

object Graph extends Controller {

  def index(environment: String, soapAction: String) = Action {
    Ok(views.html.graph.index())
  }

  // use by Json : from scala to json
  implicit object ReponseTimeWrites extends Writes[(Date, Long)] {
    def writes(data: (Date, Long)): JsValue = JsObject(List("date" -> JsString(data._1.toString), "time" -> JsNumber(data._2)))
  }

  def plot(environment: String, soapAction: String) = Action {
    val responsesTimesByDate: List[(Date, Long)] = RequestData.findResponseTimes(environment, soapAction)
    Ok(Json.toJson(responsesTimesByDate)).as(JSON)
  }

}
