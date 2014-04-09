package controllers.admin

import models._
import play.api._
import libs.json._
import play.api.mvc._

object SoapActions extends Controller {

  implicit val soapActionFormat = Json.format[SoapAction]

  /**
   * List to Datable table.
   *
   * @return JSON
   */
  def listDatatable = Action {
    implicit request =>
      val data = SoapAction.list
      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(data.size),
        "iTotalDisplayRecords" -> Json.toJson(data.size),
        "data" -> Json.toJson(data)
      ))).as(JSON)
  }

  /**
   * Display the 'edit form' of a existing SoapAction.
   *
   * @param id Id of the soapAction to edit
   */
  def edit(id: Long) = Action {
    SoapAction.findById(id).map {
      soapAction =>
        Ok(Json.toJson(soapAction)).as(JSON)
    }.getOrElse(NotFound)
  }

  /**
   * Update a soapAction.
   */
  def save(id: Long) = Action(parse.json) {
    request =>
      request.body.validate(soapActionFormat).map {
        soapAction =>
          SoapAction.update(soapAction)
          Ok(Json.toJson("Succesfully save soapAction."))
      }.recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }

  /**
   * Return all SoapActions in Json Format
   * @return JSON
   */
  def findAll = Action {
    implicit request =>
      val data = SoapAction.list
      Ok(Json.toJson(data)).as(JSON)
  }

  def regenerate() = Action {
    implicit request =>
      RequestData.soapActionOptions.foreach {
        soapAction =>
          if (SoapAction.findByName(soapAction._1) == None) {
            Logger.debug("SoapAction not found. Insert in db")
            SoapAction.insert(new SoapAction(-1, soapAction._1, 30000))
          } else {
            Logger.debug("SoapAction found. Do nothing.")
          }
      }
      Ok(Json.toJson("Success regeneration"))
  }

}
