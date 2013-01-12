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

object Services extends Controller {

  // use by Json : from scala to json
  private implicit object StatsDataWrites extends Writes[(Service, Environment)] {
    def writes(data: (Service, Environment)): JsValue = {
      JsObject(
        List(
          "0" -> JsString(data._1.description),
          "1" -> JsString(data._2.name),
          "2" -> JsString(data._1.localTarget),
          "3" -> JsString(data._1.localTarget),
          "4" -> JsString(data._1.remoteTarget),
          "5" -> JsNumber(data._1.timeoutms),
          "6" -> JsBoolean(data._1.recordXmlData),
          "7" -> JsString("<a href=\"services/"+data._1.id+"\"><i class=\"icon-edit\"></i> Edit</a>")
        ))
    }
  }

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Services.index)

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
      "environment" -> longNumber)(Service.apply)(Service.unapply))

  /**
   * Display the list of services.
   */
  def index = Action { implicit request =>
    Ok(views.html.services.index())
  }

  /**
   * List to Datable table.
   *
   * @return JSON
   */
  def listDatatable = Action { implicit request =>
    val data = Service.list
    Ok(Json.toJson(Map(
      "iTotalRecords" -> Json.toJson(data.size),
      "iTotalDisplayRecords" -> Json.toJson(data.size),
      "aaData" -> Json.toJson(data)
     ))).as(JSON)
  }

  /**
   * Display the 'edit form' of a existing Service.
   *
   * @param id Id of the service to edit
   */
  def edit(id: Long) = Action {
    Service.findById(id).map { service =>
      Ok(views.html.services.editForm(id, serviceForm.fill(service), Environment.options))
    }.getOrElse(NotFound)
  }

  /**
   * Handle the 'edit form' submission
   *
   * @param id Id of the service to edit
   */
  def update(id: Long) = Action { implicit request =>
    serviceForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.services.editForm(id, formWithErrors, Environment.options)),
      service => {
        Service.update(id, service)
        Home.flashing("success" -> "Service %s has been updated".format(service.description))
      })
  }

  /**
   * Display the 'new service form'.
   */
  def create = Action {
    Ok(views.html.services.createForm(serviceForm, Environment.options))
  }

  /**
   * Handle the 'new service form' submission.
   */
  def save = Action { implicit request =>
    serviceForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.services.createForm(formWithErrors, Environment.options)),
      service => {
        Service.insert(service)
        Home.flashing("success" -> "Service %s has been created".format(service.description))
      })
  }

  /**
   * Handle service deletion.
   */
  def delete(id: Long) = Action {
    Service.delete(id)
    Home.flashing("success" -> "Service has been deleted")
  }

}
