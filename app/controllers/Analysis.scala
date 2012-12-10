package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._

import models._
import models.UtilDate._
import java.util.Date

object Analysis extends Controller {

  def index(environment: String, soapAction: String, minDate: String, maxDate : String, status : String) = Action {
    implicit request =>
      Ok(views.html.analysis.index(environment, soapAction, formatDate(getDate(minDate)), formatDate(getDate(maxDate)), status, Environment.options, RequestData.soapActionOptions, RequestData.statusOptions))
  }

  // use by Json : from scala to json
  implicit object ReponseTimeWrites extends Writes[(Long, String, Date, Long)] {
    def writes(data: (Long, String, Date, Long)): JsValue = JsObject(
      List("e" -> JsString(Environment.options.find(t => t._1 == data._1.toString).get._2),
        "a" -> JsString(data._2),
        "d" -> JsNumber(data._3.getTime),
        "t" -> JsNumber(data._4))
    )
  }

  def load(environment: String, soapAction: String, minDate: String, maxDate : String, status: String) = Action {
    val responsesTimesByDate = RequestData.findResponseTimes(environment, soapAction, getDate(minDate).getTime, getDate(maxDate, v23h59min59s).getTime, status)
    Ok(Json.toJson(responsesTimesByDate)).as(JSON)
  }

}
