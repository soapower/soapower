package controllers

import play.api.mvc._

import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

/**
 * This controller handle all operations related to groups
 */

object Groups extends Controller {

  // use by Json : from scala to json
  private implicit object GroupsOptionsDataWrites extends Writes[(String, String)] {
    def writes(data: (String, String)): JsValue = {
      JsObject(
        List(
          "id" -> JsString(data._1),
          "name" -> JsString(data._2)
        ))
    }
  }

  /**
   * Group format
   */
  implicit val groupFormat = Json.format[Group]

  /**
   * Retrieve all groups and generate a DataTable's JSON format in order to be displayed in a datatable
   *
   * @return A group JSON datatable data
   */
  def listDatatable = Action {
    implicit request =>
      val data = Group.findAll
      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(data.size),
        "iTotalDisplayRecords" -> Json.toJson(data.size),
        "data" -> Json.toJson(data)
      ))).as(JSON)
  }

  /**
   * Return all Groups in Json Format.
   * @return JSON
   */
  def findAll = Action {
    implicit request =>
      val data = Group.findAll
      Ok(Json.toJson(data)).as(JSON)
  }

  /**
   * Return all groups options
   */
  def options = Action {
    implicit request =>
      val data = Group.options
      Ok(Json.toJson(data)).as(JSON)
  }


  /**
   * Display the 'edit form' of a existing Group.
   *
   * @param id Id of the group to edit
   */
  def edit(id: Long) = Action {
    Group.findById(id).map {
      group =>
        Ok(Json.toJson(group)).as(JSON)
    }.getOrElse(NotFound)
  }


  /**
   * Insert or update a group.
   */
  def save(id: Long) = Action(parse.json) {
    request =>
      request.body.validate(groupFormat).map {
        group =>
          try {
            if (id < 0) Group.insert(group)
            else Group.update(group)
            Ok(Json.toJson("Succesfully save group " + id))
          } catch {
            case e : Throwable => { BadRequest("Detected error:" + e.getMessage) }
          }
      }.recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }

  /**
   * Handle group deletion.
   */
  def delete(id: Long) = Action(parse.tolerantText) { request =>
    if (id == Group.ID_DEFAULT_GROUP) BadRequest("failure : Default group can't be deleted")
    else {
      val groupOption = Group.findById(id)
      groupOption match {
        case Some(group) =>
          Group.delete(group)
          Ok("deleted")
        case None =>
          Ok("failure : Group doesn't exist")
      }
    }

  }

}
