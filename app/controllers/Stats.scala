package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import models.UtilDate._
import play.api.libs.iteratee.Enumerator
import play.api.http.HeaderNames
import scala.xml.PrettyPrinter
import org.xml.sax.SAXParseException
import java.util.Date

object Stats extends Controller {

  // use by Json : from scala to json
  private implicit object StatsDataWrites extends Writes[(String, Long, Long)] {
    def writes(data: (String, Long, Long)): JsValue = {
      JsObject(
        List(
          "0" -> JsString(data._1),
          "1" -> JsNumber(data._2),
          "2" -> JsNumber(data._3)))
    }
  }

  def index(environment: String, minDateAsStr: String, maxDateAsStr: String, soapAction: String, status: String) = Action { implicit request =>
    val minDate = getDate(minDateAsStr)
    val maxDate = getDate(maxDateAsStr)
    Ok(views.html.stats.index(environment, soapAction, formatDate(minDate), formatDate(maxDate), status, Environment.options, RequestData.soapActionOptions, RequestData.statusOptions))
  }

  def listDataTable(environmentName: String, minDateAsStr: String, maxDateAsStr: String, soapAction: String, status: String) = Action { implicit request =>
    val environmentId = Environment.options.find(_._2 == environmentName).map(_._1.toLong)

    if (environmentId.nonEmpty) {
      // load thresholds
      val thresholdsBySoapActions = SoapAction.loadAll().map(action => (action.name, action.thresholdms)).toMap

      // compute average response times
      val minDate = getDate(minDateAsStr).getTime()
      val maxDate = getDate(maxDateAsStr, v23h59min59s).getTime()
      val avgResponseTimesByAction = RequestData.loadAvgResponseTimesByAction(environmentId.get, minDate, maxDate, true)

      val data = avgResponseTimesByAction.map(d => (d._1, d._2, thresholdsBySoapActions.getOrElse[Long](d._1, -1)))

      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(data.size),
        "iTotalDisplayRecords" -> Json.toJson(data.size),
        "aaData" -> Json.toJson(data)))).as(JSON)
    } else {
      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(0),
        "iTotalDisplayRecords" -> Json.toJson(0),
        "aaData" -> Json.toJson(("", 0L, 0L))))).as(JSON)
    }
  }

}
