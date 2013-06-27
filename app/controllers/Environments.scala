package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import anorm._

import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

object Environments extends Controller {

  // use by Json : from scala to json
  private implicit object EnvironmentsDataWrites extends Writes[Environment] {
    def writes(data: Environment): JsValue = {
      JsObject(
        List(
          "name" -> JsString(data.name),
          "hourRecordXmlDataMin" -> JsString(data.hourRecordXmlDataMin + " h"),
          "hourRecordXmlDataMax" -> JsString(data.hourRecordXmlDataMax + " h"),
          "nbDayKeepXmlData" -> JsString(data.nbDayKeepXmlData + " days"),
          "nbDayKeepAllData" -> JsString(data.nbDayKeepAllData + " days"),
          "recordXmlData" -> JsBoolean(data.recordXmlData),
          "recordData" -> JsBoolean(data.recordData),
          "edit" -> JsString(data.id.toString)
        ))
    }
  }

  // use by Json : from scala to json
  private implicit object EnvironmentsOptionsDataWrites extends Writes[(String, String)] {
    def writes(data : (String, String)): JsValue = {
      JsObject(
        List(
          "id" -> JsString(data._1),
          "name" -> JsString(data._2)
        ))
    }
  }

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Environments.index)

  /**
   * Display the list of services.
   */
  def index = Action { implicit request =>
    Ok(views.html.environments.index())
  }

  /**
   * List to Datable table.
   *
   * @return JSON
   */
  def listDatatable = Action { implicit request =>
    val data = Environment.list
    Ok(Json.toJson(Map(
      "iTotalRecords" -> Json.toJson(data.size),
      "iTotalDisplayRecords" -> Json.toJson(data.size),
      "aaData" -> Json.toJson(data)
    ))).as(JSON)
  }

  /**
   * Return all Environments in Json Format
   * @return JSON
   */
  def findAll = Action { implicit request =>
    val data = Environment.list
    Ok(Json.toJson(data)).as(JSON)
  }

  /**
   * Return all Environments in Json Format
   * @return JSON
   */
  def options = Action { implicit request =>
    val data = Environment.options
    Ok(Json.toJson(data)).as(JSON)
  }

  /**
   * Describe the environment form (used in both edit and create screens).
   */
  val environmentForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "name" -> nonEmptyText,
      "hourRecordXmlDataMin" -> number(min=0, max=23),
      "hourRecordXmlDataMax" -> number(min=0, max=24),
      "nbDayKeepXmlData" -> number(min=0, max=10),
      "nbDayKeepAllData" -> number(min=2, max=50),
      "recordXmlData" -> boolean,
      "recordData" -> boolean) (Environment.apply)(Environment.unapply))

  /**
   * Display the 'edit form' of a existing Environment.
   *
   * @param id Id of the environment to edit
   */
  def edit(id: Long) = Action {
    Environment.findById(id).map { environment =>
      Ok(views.html.environments.editForm(id, environmentForm.fill(environment)))
    }.getOrElse(NotFound)
  }

  /**
   * Handle the 'edit form' submission
   *
   * @param id Id of the environment to edit
   */
  def update(id: Long) = Action { implicit request =>
    environmentForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.environments.editForm(id, formWithErrors)),
      environment => {
        Environment.update(id, environment)
        Home.flashing("success" -> "Environment %s has been updated".format(environment.name))
      })
  }

  /**
   * Display the 'new environment form'.
   */
  def create = Action {
    Ok(views.html.environments.createForm(environmentForm.fill(new Environment(NotAssigned, "", 6, 22, 2, 5))))
  }

  /**
   * Handle the 'new environment form' submission.
   */
  def save = Action { implicit request =>
    environmentForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.environments.createForm(formWithErrors)),
      environment => {
        Environment.insert(environment)
        Home.flashing("success" -> "Environment %s has been created".format(environment.name))
      })
  }

  /**
   * Handle environment deletion.
   */
  def delete(id: Long) = Action {
    Environment.delete(id)
    Home.flashing("success" -> "Environment has been deleted")
  }

}
