package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import anorm._

import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import play.api.Play

object Services extends Controller {

  // use by Json : from scala to json
  private implicit object ServicesDataWrites extends Writes[(Service, Environment)] {
    def writes(data: (Service, Environment)): JsValue = {
      JsObject(
        List(
          "id" -> JsString(data._1.id.toString),
          "description" -> JsString(data._1.description),
          "env" -> JsString(data._2.name),
          "localTarget" -> JsString("/soap/" + data._2.name + "/" + data._1.localTarget),
          "remoteTarget" -> JsString(data._1.remoteTarget),
          "timeoutInMs" -> JsNumber(data._1.timeoutms),
          "recordXmlData" -> JsBoolean(data._1.recordXmlData),
          "recordData" -> JsBoolean(data._1.recordData)
        ))
    }
  }

  // use by Json : from scala to json
  private implicit object SerivceDataWrites extends Writes[Service] {
    def writes(service: Service): JsValue = {
      JsObject(
        List(
          "id" -> JsString(service.id.toString),
          "description" -> JsString(service.description),
          "env" -> JsNumber(service.environmentId),
          "localTarget" -> JsString(service.localTarget),
          "remoteTarget" -> JsString(service.remoteTarget),
          "timeoutInMs" -> JsNumber(service.timeoutms),
          "recordXmlData" -> JsBoolean(service.recordXmlData),
          "recordData" -> JsBoolean(service.recordData)
        )
      )
    }
  }

  /**
   * Describe the service form (used in both edit and create screens).
   */
  val serviceForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
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
   * Handle the 'edit form' submission
   *
   * @param id Id of the service to edit
   */
  def update(id: Long) = Action {
    implicit request =>
      serviceForm.bindFromRequest.fold(
        formWithErrors => BadRequest("Error update"),//BadRequest(views.html.services.editForm(id, formWithErrors, Environment.options)),
        service => {
          Service.update(id, service)
          //Home.flashing("success" -> "Service %s has been updated".format(service.description))
          Ok(Json.toJson(service)).as(JSON)
        })
  }

  /**
   * Handle the 'new service form' submission.
   */
  def save = Action {
    implicit request =>
      serviceForm.bindFromRequest.fold(
        formWithErrors => BadRequest("Error save"),//BadRequest(views.html.services.createForm(formWithErrors, Environment.options)),
        service => {
          Service.insert(service)
          //Home.flashing("success" -> "Service %s has been created".format(service.description))
          Ok(Json.toJson(service)).as(JSON)
        })
  }

  /**
   * Handle service deletion.
   */
  def delete(id: Long) = Action {
    Service.delete(id)
    Ok("deleted");
  }

}
