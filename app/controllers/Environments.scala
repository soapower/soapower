package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

object Environments extends Controller {

  // use by Json : from scala to json
  private implicit object EnvironmentsOptionsDataWrites extends Writes[(String, String)] {
    def writes(data : (String, String)): JsValue = {
  private implicit object StatsDataWrites extends Writes[(Environment, Group)] {
    def writes(data: (Environment, Group)): JsValue = {
      JsObject(
        List(
          "id" -> JsString(data._1),
          "name" -> JsString(data._2)
          "0" -> JsString(data._1.name),
          "1" -> JsString(data._1.hourRecordXmlDataMin + " h"),
          "2" -> JsString(data._1.hourRecordXmlDataMax + " h"),
          "3" -> JsString(data._1.nbDayKeepXmlData + " days"),
          "4" -> JsString(data._1.nbDayKeepAllData + " days"),
          "5" -> JsBoolean(data._1.recordXmlData),
          "6" -> JsBoolean(data._1.recordData),
          "7" -> JsString(data._2.groupName),
          "8" -> JsString("<a href=\"environments/"+data._1.id+"\"><i class=\"icon-edit\"></i> Edit</a>")
        ))
    }
  }

  implicit val environmentFormat = Json.format[Environment]

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
   * List to Datable table.
   *
   * @return JSON
   */
  def listDatatable = Action {
    implicit request =>
      val data = Environment.list
      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(data.size),
        "iTotalDisplayRecords" -> Json.toJson(data.size),
        "data" -> Json.toJson(data)
      ))).as(JSON)
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
      "recordData" -> boolean,
      "groupId" -> longNumber  
    ) (Environment.apply)(Environment.unapply))

  /**
   * Display the 'edit form' of a existing Environment.
   *
   * @param id Id of the environment to edit
   */
  def edit(id: Long) = Action {
    Environment.findById(id).map {
      environment =>
        Ok(Json.toJson(environment)).as(JSON)
    Environment.findById(id).map { environment =>
      Ok(views.html.environments.editForm(id, environmentForm.fill(environment), Group.options))
    }.getOrElse(NotFound)
  }

  /**
   * Insert or update a environment.
   */
  def save(id: Long) = Action(parse.json) { request =>
    request.body.validate(environmentFormat).map { environment =>
      if (id < 0) Environment.insert(environment)
      else Environment.update(environment)
      Ok(Json.toJson("Succesfully save environment."))
    }.recoverTotal{
      e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
    }
  def update(id: Long) = Action { implicit request =>
    environmentForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.environments.editForm(id, formWithErrors, Group.options)),
      environment => {
        Environment.update(id, environment)
        Home.flashing("success" -> "Environment %s has been updated".format(environment.name))
      })
  }

  /**
   * Display the 'new environment form'.
   */
  def create = Action {
    Ok(views.html.environments.createForm(environmentForm.fill(new Environment(NotAssigned, "", 6, 22, 2, 5, true, true, Group.getDefaultGroup.groupId.get)), Group.options))
  }

  /**
   * Handle the 'new environment form' submission.
   */
  def save = Action { implicit request =>
    environmentForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.environments.createForm(formWithErrors, Group.options)),
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
    Ok("deleted");
  }
}
}
