package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.UtilDate._
import play.api.Logger

object Stats extends Controller {

  // use by Json : from scala to json
  private implicit object StatsDataWrites extends Writes[(String, String, Long, Long)] {
    def writes(data: (String, String, Long, Long)): JsValue = {
      JsObject(
        List(
          "0" -> JsString(data._1),
          "1" -> JsString(data._2),
          "2" -> JsNumber(data._3),
          "3" -> JsNumber(data._4)))
    }
  }

  def index(group : String, environment: String, minDateAsStr: String, maxDateAsStr: String, soapAction: String, status: String) = Action { implicit request =>
    val minDate = getDate(minDateAsStr)
    val maxDate = getDate(maxDateAsStr)
    Ok(views.html.stats.index(group, environment, soapAction, formatDate(minDate), formatDate(maxDate), status, Group.options, Environment.options(group), RequestData.soapActionOptions, RequestData.statusOptions))
  }

  def listDataTable(environmentName: String, minDateAsStr: String, maxDateAsStr: String, soapAction: String, status: String) = Action { implicit request =>
    // load thresholds
    val thresholdsBySoapActions = SoapAction.loadAll().map(action => (action.name, action.thresholdms)).toMap

    // compute average response times
    val minDate = getDate(minDateAsStr).getTime()
    val maxDate = getDate(maxDateAsStr, v23h59min59s).getTime()
    val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(environmentName, minDate, maxDate, true)

    val data = avgResponseTimesByAction.map(d => (environmentName, d._1, d._2, thresholdsBySoapActions.getOrElse[Long](d._1, -1)))

    Ok(Json.toJson(Map(
      "iTotalRecords" -> Json.toJson(data.size),
      "iTotalDisplayRecords" -> Json.toJson(data.size),
      "aaData" -> Json.toJson(data)))).as(JSON)
  }

  def statsAsJunit(minDateAsStr: String, maxDateAsStr: String) = Action {
    val minDate = getDate(minDateAsStr).getTime()
    val maxDate = getDate(maxDateAsStr, v23h59min59s).getTime()
    val thresholdsBySoapActions = SoapAction.loadAll().map(action => (action.name, action.thresholdms)).toMap

    var ret = ""
    Environment.options.foreach{e =>
      val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(e._1, minDate, maxDate, true)
      val data = avgResponseTimesByAction.map(d => (d._1, d._2, thresholdsBySoapActions.getOrElse[Long](d._1, -1)))

      ret += "<testsuite name=\""+e._2+"\">"
      data.foreach{ d =>
        ret += "<testcase classname='"+d._1+"' name='"+d._1+"_on_"+e._2+"' time='"+(d._2.toFloat/1000).toFloat+"'>"
        if (d._2 > d._3) ret += "<failure type='NotEnoughFoo'> Response Time > Threshold: "+d._2+" > "+d._3+" </failure>"
        ret += "</testcase>"
      }
      ret += "</testsuite>"

    }

    ret = "<testsuites>" + ret + "</testsuites>"

    Ok(ret).as(XML)
  }

}
