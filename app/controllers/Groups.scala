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

/**
 * This controller handle all operations related to groups
 */

object Groups extends Controller {

  // use by Json : from scala to json
  private implicit object StatsDataWrites extends Writes[Group] {
    def writes(groupToWrite: Group): JsValue = {
      JsObject(
        List(
          "0" -> JsString(groupToWrite.name),
          "1" -> JsString("<a href=\"groups/"+groupToWrite.id+"\"><i class=\"icon-edit\"></i> Edit</a>")
        ))
    }
  }

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Groups.index)

  /**
   * Display the list of groups.
   */
  def index = Action { implicit request =>
    Ok(views.html.groups.index())
  }

  /**
   * Retrieve all groups and generate a DataTable's JSON format in order to be displayed in a datatable
   *
   * @return A group JSON datatable data
   */
  def listDatatable = Action { implicit request =>
    val data = Group.allGroups
    Ok(Json.toJson(Map(
      "iTotalRecords" -> Json.toJson(data.size),
      "iTotalDisplayRecords" -> Json.toJson(data.size),
      "aaData" -> Json.toJson(data)
    ))).as(JSON)
  }

  /**
   * Describe the group form (used in both edit and create screens).
   */
  val groupForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "name" -> nonEmptyText)
      (Group.apply)(Group.unapply))

  /**
   * Display the 'edit form' of a existing Group.
   *
   * @param id Id of the group to edit
   */
  def edit(id: Long) = Action {
    Group.findById(id).map { group =>
      Ok(views.html.groups.editForm(id, groupForm.fill(group)))
    }.getOrElse(NotFound)
  }

  /**
   * Handle the 'edit form' submission
   *
   * @param id Id of the group to edit
   */
  def update(id: Long) = Action { implicit request =>
    groupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.groups.editForm(id, formWithErrors)),
      group => {
        group.update(group)
        Home.flashing("success" -> "Group %s has been updated".format(group.name))
      })
  }

  /**
   * Display the 'new group form'.
   */
  def create = Action {
    Ok(views.html.groups.createForm(groupForm.fill(new Group(NotAssigned, ""))))
  }

  /**
   * Handle the 'new group form' submission.
   */
  def save = Action { implicit request =>
    groupForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.groups.createForm(formWithErrors)),
      group => {
        Group.insert(group)
        Home.flashing("success" -> "Group %s has been created".format(group.name))
      })
  }

  /**
   * Handle group deletion.
   */
  def delete(id: Long) = Action {
    val groupOption = Group.findById(id)
    groupOption match{
      case Some(group) =>
        Group.delete(group)
        Home.flashing("success" -> "Group has been deleted")
      case None =>
         Home.flashing("failure" -> "Group doesn't exist")
    }

  }

}
