package controllers

import play.api.mvc._
import play.api.libs.json._

import models._
import models.UtilDate._
import java.util.Date

import collection.mutable.Map
import play.Logger

object Analysis extends Controller {

  // use by Json : from scala to json
  implicit object ReponseTimeWrites extends Writes[(Long, String, Date, Long)] {
    def writes(data: (Long, String, Date, Long)): JsValue = JsObject(
      List(
        "responseTime" -> JsNumber(data._4),
        "env" -> JsString(Environment.optionsAll.find(t => t._1 == data._1.toString).get._2),
        "soapaction" -> JsString(data._2),
        "x" -> JsNumber(data._3.getTime)
        )
    )
  }

  implicit object TupleWrites extends Writes[(Long, Long)] {
    def writes(data: (Long, Long)): JsValue = JsArray(
      Seq(JsNumber(data._1), JsNumber(data._2))
    )
  }

  implicit object formatWrites extends Writes[Map[String, Entity]] {
    def writes(data: Map[String, Entity]): JsValue = {

      var x = JsArray()

      data.foreach{ d =>
        x ++= Json.arr(
          Json.obj(
            "key" -> d._1,
            "values" -> d._2.tuples
          )
        )

      }
      Logger.debug("x =>" + x)
      x
    }
  }

  class Entity(soapAction: String, var tuples:List[(Long, Long)])

  def load(groupName: String, environment: String, soapAction: String, minDate: String, maxDate : String, status: String, statsOnly: String) = Action {
    val responsesTimesByDate = RequestData.findResponseTimes(groupName, environment, soapAction, getDate(minDate).getTime, getDate(maxDate, v23h59min59s).getTime, status, true)

    val a:Map[String, Entity] = Map()

    responsesTimesByDate.foreach{ r =>
      if (a.contains(r._2)) {
        (a.get(r._2).get).tuples ++= List((r._3.getTime, r._4))
      } else {
        (a.put(r._2, new Entity(r._2, List((r._3.getTime, r._4)))))
      }
    }

    Ok(Json.toJson(a)).as(JSON)
  }

}

