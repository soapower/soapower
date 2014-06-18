package controllers.admin

import play.api.mvc._
import models._
import play.api.libs.iteratee._
import play.api.http._
import java.util.{Calendar, GregorianCalendar, TimeZone, Date}
import play.api._
import play.api.libs.json.{JsError, Json}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import java.text.SimpleDateFormat

object Admin extends Controller {

  case class AdminForm(environmentName: String, minDate: String, maxDate: String, typeAction: String)

  def toJson(code: String, text: String) = Json.obj("code" -> code, "text" -> text)

  implicit val adminFormat = Json.format[AdminForm]

  def delete = Action(parse.json) {
    request =>
      request.body.validate(adminFormat).map {
        adminForm =>
          val format = new SimpleDateFormat("yyyy-MM-dd HH:mm")
          val correctMinDate = format.parse(adminForm.minDate)
          val correctMaxDate = format.parse(adminForm.maxDate)

          adminForm.typeAction match {
            case "xml-data" =>
              RequestData.deleteRequestResponse(adminForm.environmentName, correctMinDate, correctMaxDate, "Admin Soapower")
            case "all-data" =>
              RequestData.delete(adminForm.environmentName, correctMinDate, correctMaxDate)
          }
          Ok(toJson("success", "Success deleting data")).as(JSON)
      }.recoverTotal {
        e => Ok(toJson("error", "Detected error:" + JsError.toFlatJson(e))).as(JSON)
      }
  }
}
