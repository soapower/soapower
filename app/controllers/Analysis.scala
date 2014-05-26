package controllers

import play.api.mvc._
import play.api.libs.json._

import models._
import models.UtilDate._
import java.util.Date
import scala.concurrent.{ExecutionContext, Await}
import ExecutionContext.Implicits.global
import collection.mutable.Map
import play.Logger
import scala.concurrent.Await
import scala.concurrent.duration._
import models.Stat.AnalysisEntity

object Analysis extends Controller {

  // use by Json : from scala to json
  implicit object ReponseTimeWrites extends Writes[(Long, String, Date, Long)] {
    def writes(data: (Long, String, Date, Long)): JsValue = JsObject(
      List(
        "responseTime" -> JsNumber(data._4),
        "env" -> JsString(Environment.options.find(t => t._1 == data._1.toString).get._2),
        "serviceaction" -> JsString(data._2),
        "x" -> JsNumber(data._3.getTime)
      )
    )
  }

  implicit object TupleWrites extends Writes[(Long, Long)] {
    def writes(data: (Long, Long)): JsValue = JsArray(
      Seq(JsNumber(data._1), JsNumber(data._2))
    )
  }

  implicit object AnalysisEntityWrites extends Writes[List[AnalysisEntity]] {
    def writes(data: List[AnalysisEntity]): JsValue = {
      var x = JsArray()

      data.foreach {
        analysis =>
          x ++= Json.arr(
            Json.obj(
              "key" -> JsString(analysis.serviceAction + " [" + analysis.groups.mkString(", ") + "]"),
              "values" -> analysis.dateAndAvg
            )
          )
      }
      x
    }
  }

  def load(groupName: String, environment: String, serviceAction: String, minDate: String, maxDate: String, live: Boolean) = Action.async {
    Logger.debug(live.toString)
    if (!live) {
      Stat.findResponseTimes(groupName, environment, serviceAction, getDate(minDate).getTime, getDate(maxDate, v23h59min59s, true).getTime).map {
        list =>
          Ok(Json.toJson(list))
      }
    } else {
      RequestData.findResponseTimes(groupName, environment, serviceAction, getDate(minDate).getTime, getDate(maxDate, v23h59min59s, true).getTime).map {
        list =>
          Ok(Json.toJson(list))
      }
    }
  }


}


