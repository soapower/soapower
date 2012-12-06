package controllers

import play.api.mvc._
import play.api.libs.json._

import models._
import java.util.Date

object Analysis extends Controller {

  def index(environment: String, soapAction: String) = Action {
    implicit request =>
      Ok(views.html.analysis.index(environment, soapAction, Environment.options, RequestData.soapActionOptions))
  }

  // use by Json : from scala to json
  implicit object ReponseTimeWrites extends Writes[(Date, Long)] {
    def writes(data: (Date, Long)): JsValue = JsObject(List("date" -> JsNumber(data._1.getTime), "time" -> JsNumber(data._2)))
  }

  def load(environment: String, soapAction: String) = Action {
    val responsesTimesByDate: List[(Date, Long)] = RequestData.findResponseTimes(environment, soapAction)
    Ok(Json.toJson(responsesTimesByDate)).as(JSON)
  }

}
