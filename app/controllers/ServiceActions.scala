package controllers

import models._
import play.api._
import libs.json._
import play.api.mvc._

object ServiceActions extends Controller {

  implicit val serviceActionFormat = Json.format[ServiceAction]

  /**
   * List to Datable table.
   *
   * @return JSON
   */
  def listDatatable = Action {
    implicit request =>
      val data = ServiceAction.list
      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(data.size),
        "iTotalDisplayRecords" -> Json.toJson(data.size),
        "data" -> Json.toJson(data)
      ))).as(JSON)
  }

  /**
   * Display the 'edit form' of an existing ServiceAction.
   *
   * @param id Id of the serviceAction to edit
   */
  def edit(id: Long) = Action {
    ServiceAction.findById(id).map {
      serviceAction =>
        Ok(Json.toJson(serviceAction)).as(JSON)
    }.getOrElse(NotFound)
  }

  /**
   * Update a serviceAction.
   */
  def save(id: Long) = Action(parse.json) {
    request =>
      request.body.validate(serviceActionFormat).map {
        serviceAction =>
          ServiceAction.update(serviceAction)
          Ok(Json.toJson("Succesfully save serviceAction."))
      }.recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }

  /**
   * Return all ServiceActions in Json Format
   * @return JSON
   */
  def findAll = Action {
    implicit request =>
      val data = ServiceAction.list
      Ok(Json.toJson(data)).as(JSON)
  }

  def regenerate() = Action {
    implicit request =>
      RequestData.serviceActionOptions.foreach {
        serviceAction =>
          if (ServiceAction.findByName(serviceAction._1) == None) {
            Logger.debug("ServiceAction not found. Insert in db")
            ServiceAction.insert(new ServiceAction(-1, serviceAction._1, 30000))
          } else {
            Logger.debug("ServiceAction found. Do nothing.")
          }
      }
      Ok(Json.toJson("Success regeneration"))
  }

}
