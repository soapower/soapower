package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.UtilDate._
import java.util.Date
import java.net.URLDecoder
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.Stat.{AnalysisEntity, PageStat}
import org.joda.time.DateTime
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.Await
import scala.concurrent.duration._


object Stats extends Controller {

  // use by Json : from scala to json
  private implicit object StatsDataWrites extends Writes[PageStat] {
    def writes(data: PageStat): JsValue = {
      JsObject(
        List(
          "groups" -> JsString("[\"" + data.groups.mkString("\", \"") + "\"]"),
          "environmentName" -> JsString(data.environmentName),
          "serviceAction" -> JsString(data.serviceAction),
          "avgInMillis" -> JsNumber(data.avgInMillis),
          "treshold" -> JsNumber(data.treshold)))
    }
  }


  def listDataTable(groupNames: String, environmentName: String, minDateAsStr: String, maxDateAsStr: String, live: Boolean) = Action.async {

    if (!live) {
      val futureDataList = Stat.find(groupNames, environmentName, getDate(minDateAsStr).getTime, getDate(maxDateAsStr, v23h59min59s, true).getTime)

      futureDataList.map {
        list =>
          Ok(Json.toJson(Map("data" -> Json.toJson(list))))
      }
    } else {
      listWithLiveCompile(groupNames, environmentName, minDateAsStr, maxDateAsStr)
    }

  }

  /**
   * Compile live stats and send it the the client statistics page
   * @param groupNames
   * @param environmentName
   * @param minDateAsStr
   * @param maxDateAsStr
   * @return
   */
  def listWithLiveCompile(groupNames: String, environmentName: String, minDateAsStr: String, maxDateAsStr: String) = {
    val futureStats = RequestData.findStatsPerDay(groupNames, environmentName, getDate(minDateAsStr).getTime, getDate(maxDateAsStr, v23h59min59s, true).getTime, true)
    val futureServiceActions = ServiceAction.findAll

    for {
      query <- futureStats
      serviceActionsResult <- futureServiceActions
    } yield {
      val serviceActions = serviceActionsResult.map { sa => ((sa.name, sa.groups), sa.thresholdms)}.toMap
      val newList = query.map {
        stat =>
          new PageStat(stat.groups, stat.environmentName, stat.serviceAction, stat.avgInMillis, serviceActions.apply((stat.serviceAction, stat.groups)))
      }.toList
      Ok(Json.toJson(Map("data" -> Json.toJson(newList))))
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
  def statsAsJunit(groupName: String, minDateAsStr: String, maxDateAsStr: String, environment: Option[String], service: Option[String], treshold: Option[Long]) = Action.async {

    val minDate = getDate(minDateAsStr).getTime
    val maxDate = getDate(maxDateAsStr, v23h59min59s, true).getTime

    val f = Stat.findResponseTimes(groupName, environment.getOrElse("all"), service.getOrElse("all"), minDate, maxDate)

    var x = ""
    f.map {
      data =>
        data.foreach {
          analysis =>
            analysis.dateAndAvg.map(
              d => {
                val t = treshold.getOrElse(ServiceAction.getThreshold(analysis.serviceAction, groupName.split(',').toList))
                x += "<testcase classname='" + groupName + "." + analysis.environment + "' name='" + analysis.serviceAction + " at " + UtilDate.getDateFormatees(new Date(d._1)) + "' time='" + (d._2.toFloat / 1000) + "'>"
                if (d._2.toLong > t) {
                  x += "<failure type='TooSlowException'> Response Time > Threshold: " + d._2.toFloat + " > " + t + " "
                  if (t <= -1) x += "Threshold not configured on Soapower"
                  x += "</failure>"
                }
                x += "</testcase>"
              }
            )
        }
        Ok("<testsuites><testsuite>" + x + "</testsuite></testsuites>").as(XML)
    }
  }

}