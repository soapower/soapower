package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.UtilDate._
import java.util.Date

object Stats extends Controller {

  // use by Json : from scala to json
  private implicit object StatsDataWrites extends Writes[(String, String, Long, Long)] {
    def writes(data: (String, String, Long, Long)): JsValue = {
      JsObject(
        List(
          "env" -> JsString(data._1),
          "serviceAction" -> JsString(data._2),
          "avgTime" -> JsNumber(data._3),
          "threshold" -> JsNumber(data._4)))
    }
  }

  def listDataTable(groupName: String, environmentName: String, minDateAsStr: String, maxDateAsStr: String, status: String) = Action {
    implicit request =>
    // load thresholds
      val thresholdsByServiceActions = ServiceAction.loadAll().map(action => (action.name, action.thresholdms)).toMap

      // compute average response times
      val minDate = getDate(minDateAsStr).getTime()
      val maxDate = getDate(maxDateAsStr, v23h59min59s).getTime()
      val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(groupName, environmentName, status, minDate, maxDate, true)

      val data = avgResponseTimesByAction.map(d => (environmentName, d._1, d._2, thresholdsByServiceActions.getOrElse[Long](d._1, -1)))

      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(data.size),
        "iTotalDisplayRecords" -> Json.toJson(data.size),
        "data" -> Json.toJson(data)))).as(JSON)
  }

  /**
   * Create a Junit XML test based on the parameters
   * @param groupName the group name of the environments to test
   * @param minDateAsStr
   * @param maxDateAsStr
   * @param environmentName the environment name to test. If this parameter is not set, statsAsJunit will test all the environment of the group.
   * @param treshold the treshold. If this parameter is not set, statsAsJunit will used ServiceActions's treshold.
   * @param percentile the percentile. If this parameter is not set, percentile will be set at 90 (default value)
   * @return
   */
  def statsAsJunit(groupName: String, minDateAsStr: String, maxDateAsStr: String, environmentName: Option[String], treshold: Option[Long], percentile: Option[Int]) = Action {
    val minDate = getDate(minDateAsStr).getTime
    val maxDate = getDate(maxDateAsStr, v23h59min59s, true).getTime

    // We retrieve the percentile from the URL query
    val realPercentile = percentile match {
      case None =>
        // Default percentile value
        90
      case _ =>
        percentile.get
    }
    if (realPercentile > 100 || realPercentile < 1) {
      val err = "The percentile parameter must have a value between 1 and 100"
      BadRequest(err)
    } else {
      environmentName match {
        case None => {
          // No environment name is defined, the Junit XML test will be on the entire group
          statsForGroup(groupName, minDate, maxDate, treshold, realPercentile)
        }
        case _ => {
          // An environment name is define, the Junit XML test will be on the actions of the environment
          statsForEnvir(groupName, environmentName.get, minDate, maxDate, treshold, realPercentile)
        }
      }
    }
  }

  def statsForGroup(groupName: String, minDate: Date, maxDate: Date, treshold: Option[Long],
                    percentile: Int) = {

    var ret = ""
    treshold match {
      case None => {
        // If the treshold parameter is not defined, we retrieve all the serviceActions' name and serviceActions' treshold
        val thresholdsByServiceActions = ServiceAction.loadAll().map(action => (action.name, action.thresholdms)).toMap
        Environment.optionsAll.foreach {
          e =>
            val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(groupName, e._1, "200", minDate, maxDate, true, percentile)
            val data = avgResponseTimesByAction.map(d => (d._1, d._2, thresholdsByServiceActions.getOrElse[Long](d._1, -1)))

            ret += "<testsuite name=\"" + e._2 + "\">"
            data.foreach {
              d =>
                ret += "<testcase classname='" + d._1 + "' name='" + d._1 + "_on_" + e._2 + "' time='" + (d._2.toFloat / 1000).toFloat + "'>"
                if (d._2 > d._3) ret += "<failure type='NotEnoughFoo'> Response Time > Threshold: " + d._2 + " > " + d._3 + " </failure>"
                ret += "</testcase>"
            }
            ret += "</testsuite>"
        }
      }
      case _ => {
        Environment.optionsAll.foreach {
          e =>
            ret += "<testsuite name=\"" + e._2 + "\">"
            val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(groupName, e._1, "200", minDate, maxDate, true, percentile)
            avgResponseTimesByAction.foreach {
              response =>
                ret += "<testcase classname='" + response._1 + "' name='" + response._1 + "_on_" + e._1 + "' time='" + (response._2.toFloat / 1000).toFloat + "'>"
                if (response._2 > treshold.get) ret += "<failure type='NotEnoughFoo'> Response Time > Threshold: " + response._2 + " > " + treshold.get + " </failure>"
                ret += "</testcase>"
            }
            ret += "</testsuite>"

        }
      }
    }
    ret = "<testsuites>" + ret + "</testsuites>"
    Ok(ret).as(XML)
  }


  /**
   * Create a Junit XML test that test if a percentile of the actions' response time of a given environment are lower than
   * the treshold in parameter. If the percentile parameter is not defined, the default value will be 90. If the treshold parameter
   * is not defined, it will test if a percentile of the actions' response time of the environment are lower than their serviceAction's treshold.
   * @param group the group of the environment
   * @param environmentName the name of the environment to test
   * @param minDate
   * @param maxDate
   * @param treshold
   * @param percentile the percentile of actions response time to test
   * @return
   */
  def statsForEnvir(group: String, environmentName: String, minDate: Date, maxDate: Date,
                    treshold: Option[Long], percentile: Int) = {

    // retrieve the correct values for minDate and maxDate from the URL

    var ret = "<testsuite name=\"" + environmentName + "\">"
    // We retrieve the average response times of the environment's actions
    val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(group, environmentName, "200", minDate, maxDate, true, percentile)

    treshold match {
      case None => {
        // If the treshold parameter is not defined, we retrieve all the serviceActions of the environment
        val thresholdsByServiceActions = ServiceAction.loadAll().map(action => (action.name, action.thresholdms)).toMap
        val data = avgResponseTimesByAction.map(d => (d._1, d._2, thresholdsByServiceActions.getOrElse[Long](d._1, -1)))

        data.foreach {
          d =>
            ret += "<testcase classname='" + d._1 + "' name='" + d._1 + "_on_" + environmentName + "' time='" + (d._2.toFloat / 1000).toFloat + "'>"
            if (d._2 > d._3) ret += "<failure type='NotEnoughFoo'> Response Time > Threshold: " + d._2 + " > " + d._3 + " </failure>"
            ret += "</testcase>"
        }
      }
      case _ => {
        avgResponseTimesByAction.foreach {
          response =>
            ret += "<testcase classname='" + response._1 + "' name='" + response._1 + "_on_" + environmentName + "' time='" + (response._2.toFloat / 1000).toFloat + "'>"
            if (response._2 > treshold.get) ret += "<failure type='NotEnoughFoo'> Response Time > Threshold: " + response._2 + " > " + treshold.get + " </failure>"
            ret += "</testcase>"
        }
      }
    }

    ret += "</testsuite>"
    ret = "<testsuites>" + ret + "</testsuites>"
    Ok(ret).as(XML)
  }
}