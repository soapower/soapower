package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import anorm.Pk

object Environments extends Controller {


  // use by Json : from scala to json
  private implicit object EnvironmentsOptionsDataWrites extends Writes[(String, String)] {
    def writes(data: (String, String)): JsValue = {
      JsObject(
        List(
          "id" -> JsString(data._1),
          "name" -> JsString(data._2)
        ))
    }
  }

  implicit val environmentFormat = Json.format[Environment]

  /**
   * Return all Environments in Json Format
   * @return JSON
   */
  def findAll = Action {
    implicit request =>
      val data = Environment.list
      Ok(Json.toJson(data)).as(JSON)
  }

  /**
   * Return all Environments in Json Format
   * @return JSON
   */
  def options(group: String) = Action {
    implicit request =>
      var data: Seq[(String, String)] = null.asInstanceOf[Seq[(String, String)]]
      if (group == "all") {
        data = Environment.optionsAll
      } else {
        data = Environment.optionsAll(group)
      }
      Ok(Json.toJson(data)).as(JSON)
  }


  /**
   * List to Datable table.
   *
   * @return JSON
   */
  def listDatatable(group: String) = Action {
    implicit request =>

      var data: List[Environment] = null.asInstanceOf[List[Environment]]

      if (group != "all") {
        data = Environment.list(group)
      } else {
        data = Environment.list
      }

      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(data.size),
        "iTotalDisplayRecords" -> Json.toJson(data.size),
        "data" -> {
          Json.toJson(data)
        }
      ))).as(JSON)


  }

  /**
   * Display the 'edit form' of a existing Environment.
   *
   * @param id Id of the environment to edit
   */
  def edit(id: Long) = Action {
    Environment.findById(id).map {
      environment =>
        Ok(Json.toJson(environment)).as(JSON)
    }.getOrElse(NotFound)
  }


  /**
   * Insert or update a environment.
   */
  def save(id: Long) = Action(parse.json) {
    request =>
      request.body.validate(environmentFormat).map {
        environment =>
          try {
            if (id < 0) Environment.insert(environment)
            else Environment.update(environment)
            Ok(Json.toJson("Succesfully save environment."))
          } catch {
            case e : Throwable => { BadRequest("Detected error:" + e.getMessage) }
          }

      }.recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }

  /**
   * Handle environment deletion.
   */
  def delete(id: Long) = Action(parse.tolerantText) { request =>
    Environment.delete(id)
    Ok("deleted")
  }
}

