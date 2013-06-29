package controllers

import play.api.data._
import play.api.data.Forms._

import play.api.mvc._
import models._
import play.api.libs.json._

object Services extends Controller {

  implicit val serviceFormat = Json.format[Service]


  /**
   * Describe the service form (used in both edit and create screens).
   */
  val serviceForm = Form(
    mapping(
      "id" -> longNumber,
      "description" -> nonEmptyText,
      "localTarget" -> nonEmptyText,
      "remoteTarget" -> nonEmptyText,
      "timeoutms" -> longNumber,
      "recordXmlData" -> boolean,
      "recordData" -> boolean,
      "environment" -> longNumber)(Service.apply)(Service.unapply))

  /**
   * List to Datable table.
   *
   * @return JSON
   */
  def listDatatable = Action {
    implicit request =>
      val data = Service.list
      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(data.size),
        "iTotalDisplayRecords" -> Json.toJson(data.size),
        "data" -> Json.toJson(data)
      ))).as(JSON)
  }

  /**
   * Display the 'edit form' of a existing Service.
   *
   * @param id Id of the service to edit
   */
  def edit(id: Long) = Action {
    Service.findById(id).map {
      service =>
        Ok(Json.toJson(service)).as(JSON)
    }.getOrElse(NotFound)
  }

  /**
   * Insert or update a service.
   */
  def save(id: Long) = Action(parse.json) { request =>
    request.body.validate(serviceFormat).map { service =>
      if (id < 0) Service.insert(service)
      else Service.update(service)
      Ok(Json.toJson("Succesfully save service."))
    }.recoverTotal{
      e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
    }
  }

  /**
   * Handle service deletion.
   */
  def delete(id: Long) = Action {
    Service.delete(id)
    Ok("deleted");
  }

}
