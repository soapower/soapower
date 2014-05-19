package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.UtilDate._
import java.util.Date
import java.net.URLDecoder
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Stats extends Controller {

  // use by Json : from scala to json
  private implicit object StatsDataWrites extends Writes[(String, String, String, Long, Long)] {
    def writes(data: (String, String, String, Long, Long)): JsValue = {
      JsObject(
        List(
          "groups" -> JsString(data._1),
          "environmentName" -> JsString(data._2),
          "serviceAction" -> JsString(data._3),
          "avgTime" -> JsNumber(data._4),
          "treshold" -> JsNumber(data._5)))
    }
  }

  def listDataTable(groupNames: String, environmentName: String, minDateAsStr: String, maxDateAsStr: String) = Action.async {
    Logger.debug(getDate(minDateAsStr).getTime.toString)
    Logger.debug(getDate(maxDateAsStr, v23h59min59s, true).getTime.toString)
    val futureDataList = Stat.find(groupNames, environmentName, getDate(minDateAsStr).getTime, getDate(maxDateAsStr, v23h59min59s, true).getTime)

    futureDataList.map {
      list =>
        Ok(Json.toJson(Map("data" -> Json.toJson(list))))
    }

  }

  /**
   * Create a Junit XML test based on the parameters
   * @param groupName the group name of the environments to test
   * @param minDateAsStr
   * @param maxDateAsStr
   * @param environment the environment name to test. If this parameter is not set, statsAsJunit will test all the environment of the group.
   * @param treshold the treshold. If this parameter is not set, statsAsJunit will used ServiceActions's treshold.
   * @return
   */
  def statsAsJunit(groupName: String, minDateAsStr: String, maxDateAsStr: String, environment: Option[String], service: Option[String], treshold: Option[Long]) = Action {

    ???
    BadRequest("TODO")
    /*
    val minDate = getDate(minDateAsStr).getTime
    val maxDate = getDate(maxDateAsStr, v23h59min59s, true).getTime

    environment match {
      case None => {
        service match {
          case None => {
            // No environment name and no services localTarget are defined, the Junit XML test will be on the entire group
            statsForGroup(groupName, minDate, maxDate, treshold)
          }
          case _ => {
            // No environment name is defined, only a service. The Junit XML test will be on every actions of the service
            statsForSingleService(groupName, service.get, minDate, maxDate, treshold)
          }
        }

      }
      case _ => {
        // An environment name is define, the Junit XML test will be on the actions of the environment
        service match {
          case None =>
            // No service name is define, the Junit XML test will test all the service of the environment
            statsForEnvir(groupName, environment.get, minDate, maxDate, treshold)
          case _ =>
            // A service name is define, the Junit XML test will test only the service of the environment
            statsForEnvirAndService(groupName, environment.get, service.get, minDate, maxDate, treshold)
        }
      }
    }
    */
  }

  /**
   * Create a Junit XML test that test if the response times of a given service for a given environment are lower than
   * the treshold in parameter. If the treshold parameter is not defined, it will test if the response times
   * are lower than the treshold define for this ServiceAction
   * @param groupName
   * @param environmentName
   * @param serviceName
   * @param minDate
   * @param maxDate
   * @param treshold
   * @return
   */
  def statsForEnvirAndService(groupName: String, environmentName: String, serviceName: String, minDate: Date, maxDate: Date, treshold: Option[Long]) = {
    ???
    /*
    val serviceAction = ServiceAction.findByName(URLDecoder.decode(serviceName, "utf-8"))
    serviceAction match {
      case None => {
        // The service action doesn't exist
        val err = "The Service Action with the name " + serviceName + " doesn't exist"
        BadRequest(err)
      }
      case _ => {
        val realTreshold = treshold match {
          case None =>
            serviceAction.get.thresholdms
          case _ =>
            treshold.get
        }
        val avgResponseTimeForActionAndEnvir = RequestData.loadAvgResponseTimesByEnvirAndAction(groupName, environmentName, serviceName, "200", minDate, maxDate, true)
        var ret = ""
        ret += "<testsuite name=\"" + serviceAction.get.name + "_on_environment_" + environmentName + "\">"
        if (avgResponseTimeForActionAndEnvir != -1) {
          ret += "<testcase classname='" + serviceAction.get.name + "' name='" + serviceAction.get.name + "' time='" + (avgResponseTimeForActionAndEnvir.toFloat / 1000).toFloat + "'>"
          if (avgResponseTimeForActionAndEnvir > realTreshold) ret += "<failure type='NotEnoughFoo'> Response Time > Threshold: " + avgResponseTimeForActionAndEnvir + " > " + realTreshold + " </failure>"
          ret += "</testcase>"
        }
        ret += "</testsuite>"
        ret = "<testsuites>" + ret + "</testsuites>"
        Ok(ret).as(XML)
      }
    }
    */
  }

  /**
   * Create a Junit XML test that test if the response times of a given service for all the environments are lower than
   * the treshold in parameter. If the treshold parameter is not defined, it will test if the response times
   * are lower than the treshold define for this ServiceAction
   * @param groupName
   * @param serviceName
   * @param minDate
   * @param maxDate
   * @param treshold
   * @return
   */
  def statsForSingleService(groupName: String, serviceName: String, minDate: Date, maxDate: Date, treshold: Option[Long]) = {
    ???
    /*
    val serviceAction = ServiceAction.findByName(URLDecoder.decode(serviceName, "utf-8"))
    serviceAction match {
      case None =>
        // The service action doesn't exist
        val err = "The Service Action with the name " + serviceName + " doesn't exist"
        BadRequest(err)

      case _ => {
        var ret = ""
        // The service action exists
        val realTreshold = treshold match {
          case None =>
            // No treshold is set, we use the serviceAction's treshold
            serviceAction.get.thresholdms
          case _ =>
            treshold.get
        }

        val avgResponseTimeForAction = RequestData.loadAvgResponseTimesBySpecificAction(groupName, serviceName, "200", minDate, maxDate, true)

        ret += "<testsuite name=\"" + serviceAction.get.name + "_on_all_environments" + "\">"
        if (avgResponseTimeForAction != -1) {
          ret += "<testcase classname='" + serviceAction.get.name + "' name='" + serviceAction.get.name + "' time='" + (avgResponseTimeForAction.toFloat / 1000).toFloat + "'>"
          if (avgResponseTimeForAction > realTreshold) ret += "<failure type='NotEnoughFoo'> Response Time > Threshold: " + avgResponseTimeForAction + " > " + realTreshold + " </failure>"
          ret += "</testcase>"
        }
        ret += "</testsuite>"
        ret = "<testsuites>" + ret + "</testsuites>"
        Ok(ret).as(XML)
      }
    }
    */
  }

  /**
   * Create a Junit XML test that test if the response times of a group are lower than
   * the treshold in parameter. If the treshold parameter is not defined, it will test if
   * the response times are lower than the treshold define for their ServiceAction
   * @param groupName
   * @param minDate
   * @param maxDate
   * @param treshold
   * @return
   */
  def statsForGroup(groupName: String, minDate: Date, maxDate: Date, treshold: Option[Long]) = {
    ???
    /*

    var ret = ""
    treshold match {
      case None => {
        // If the treshold parameter is not defined, we retrieve all the serviceActions' name and serviceActions' treshold
        val thresholdsByServiceActions = ServiceAction.loadAll().map(action => (action.name, action.thresholdms)).toMap
        Environment.optionsAll.foreach {
          e =>
            val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(groupName, e._1, "200", minDate, maxDate, true)
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
            val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(groupName, e._1, "200", minDate, maxDate, true)
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
    */
  }


  /**
   * Create a Junit XML test that test if the response times for a given environment are lower than
   * the treshold in parameter. If the treshold parameter is not defined, it will test if
   * the response times of the environment are lower than the treshold define for their ServiceAction
   * @param group the group of the environment
   * @param environmentName the name of the environment to test
   * @param minDate
   * @param maxDate
   * @param treshold
   * @return
   */
  def statsForEnvir(group: String, environmentName: String, minDate: Date, maxDate: Date,
                    treshold: Option[Long]) = {

    ???
    /*
    // retrieve the correct values for minDate and maxDate from the URL

    var ret = "<testsuite name=\"" + environmentName + "\">"
    // We retrieve the average response times of the environment's actions
    val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(group, environmentName, "200", minDate, maxDate, true)

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
    */
  }
}