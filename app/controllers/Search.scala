package controllers

import play.api.mvc._
import play.api.libs.json._
import models._
import play.api.libs.iteratee.Enumerator
import play.api.http.HeaderNames
import java.util.{Calendar, GregorianCalendar, Date}
import java.text.SimpleDateFormat

case class Search(environmentId: Long)

object Search extends Controller {

  def index(environment: String, soapAction: String, minDate: String, maxDate : String) = Action {
    implicit request =>
      Ok(views.html.search.index(environment, soapAction, formatDate(getDate(minDate)), formatDate(getDate(maxDate)), Environment.options, RequestData.soapActionOptions))
  }

  private def getDate(sDate: String, addInMillis : Long = 0):GregorianCalendar = {
    val gCal = new GregorianCalendar()

    val mDate : GregorianCalendar = sDate match {
      case "all" => gCal.setTime(RequestData.getMinStartTime.getOrElse(new Date)); gCal
      case "today" => gCal
      case "yesterday" => gCal.add( Calendar.DATE, -1 ); gCal
      case _ =>
          val f = new SimpleDateFormat("yyyy-MM-dd")
          gCal.setTime(f.parse(sDate))
          gCal
    }
    mDate.setTimeInMillis(mDate.getTimeInMillis + addInMillis)
    mDate
  }

  private def formatDate(gCal : GregorianCalendar): String = {
    var rDate = gCal.get( Calendar.YEAR )  + "-"
    rDate += addZero( gCal.get( Calendar.MONTH ) + 1 ) + "-"
    rDate += addZero(gCal.get( Calendar.DATE )) + ""
    rDate
  }

  private def addZero(f:Int): String = {
    if (f < 10) "0" + f.toString else f.toString
  }

  def listDatatable(environment: String, soapAction: String, minDate: String, maxDate: String, sSearch: String, iDisplayStart: Int, iDisplayLength: Int) = Action {
    val v23h59min59s = ((24*60*60)-1)*1000
    val page: Page[(RequestData)] = RequestData.list(environment, soapAction, getDate(minDate).getTime, getDate(maxDate, v23h59min59s).getTime, iDisplayStart, iDisplayLength, sSearch)

    Ok(Json.toJson(Map(
      "iTotalRecords" -> Json.toJson(iDisplayLength),
      "iTotalDisplayRecords" -> Json.toJson(page.total),
      "aaData" -> Json.toJson(page.items)))).as(JSON)
  }

  def downloadRequest(id: Long) = Action {
    val request = RequestData.loadRequest(id)
    val fileContent: Enumerator[String] = Enumerator(request)
    val filename = "request-" + id + ".xml"

    SimpleResult(
      header = ResponseHeader(play.api.http.Status.OK),
      body = fileContent)
      .withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename)).as(XML)
  }

  def downloadResponse(id: Long) = Action {
    val response = RequestData.loadResponse(id)

    response match {
      case Some(str: String) => {
        val fileContent: Enumerator[String] = Enumerator(str)
        val filename = "response-" + id + ".xml"
        SimpleResult(
          header = ResponseHeader(play.api.http.Status.OK),
          body = fileContent)
          .withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=" + filename)).as(XML)
      }

      case _ => NotFound("The response does not exist")
    }
  }

}
