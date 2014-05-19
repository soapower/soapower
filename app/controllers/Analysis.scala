package controllers

import play.api.mvc._
import play.api.libs.json._

import models._
import models.UtilDate._
import java.util.Date

import collection.mutable.Map
import play.Logger
import scala.concurrent.Await
import scala.concurrent.duration._

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

  implicit object TupleWrites extends Writes[(Long, Long, Long)] {
    def writes(data: (Long, Long, Long)): JsValue = JsArray(
      Seq(JsNumber(data._1), JsNumber(data._2), JsNumber(data._3))
    )
  }

  implicit object formatWrites extends Writes[Map[(List[String], String), Entity]] {
    def writes(data: Map[(List[String], String), Entity]): JsValue = {

      var x = JsArray()

      data.foreach {
        d =>
          x ++= Json.arr(
            Json.obj(
              "key" -> JsString(d._1._1.mkString(", ") + " " + d._1._2),
              "values" -> d._2.tuples
            )
          )

      }
      Logger.debug("x =>" + x)
      x
    }
  }

  // class containing a Tuple : date, nb_request, avg_time
  case class Entity(tuples : List[(Long, Long, Long)])

  def load(groupName: String, environment: String, serviceAction: String, minDate: String, maxDate: String, status: String, statsOnly: String) = Action {

    val responseTimesByData = Stat.findResponseTimes(groupName, environment, serviceAction, getDate(minDate).getTime, getDate(maxDate, v23h59min59s).getTime, status)

    var a: Map[(List[String],String), Entity] = Map()
    // GROUP BY GROUPS
    val statsByGroups = Await.result(responseTimesByData, 1.second).groupBy(_.groups)
    statsByGroups.foreach {
      statsForGroups =>
        statsForGroups._2.foreach {
          stat =>
            if(a.contains((stat.groups, stat.serviceAction))) {
              val list = List((stat.atDate.getMillis, stat.nbOfRequestData, stat.avgInMillis)) ++ (a.get(stat.groups, stat.serviceAction).get).tuples
              a.put((stat.groups, stat.serviceAction), new Entity(list) )
            } else {
              a.put((stat.groups, stat.serviceAction), new Entity(List((stat.atDate.getMillis, stat.nbOfRequestData, stat.avgInMillis))))
            }
        }
    }
    Logger.debug(a.toString);
    Ok(Json.toJson(a)).as(JSON)
  }

}

