package controllers

import play.api.mvc._

import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

/**
 * This controller handle all operations related to mockgroups
 */

object MockGroups extends Controller {

  // use by Json : from scala to json
  private implicit object MockgroupsOptionsDataWrites extends Writes[(String, String)] {
    def writes(data: (String, String)): JsValue = {
      JsObject(
        List(
          "id" -> JsString(data._1),
          "name" -> JsString(data._2)
        ))
    }
  }

  /**
   * Mockgroup format
   */
  implicit val mockgroupFormat = Json.format[MockGroup]

  /**
   * Retrieve all mockgroups and generate a DataTable's JSON format in order to be displayed in a datatable
   *
   * @return A mockgroup JSON datatable data
   */
  def listDatatable(group: String) = Action {
    implicit request =>

      var data: List[MockGroup] = null.asInstanceOf[List[MockGroup]]

      if (group != "all") {
        data = MockGroup.list(group)
      } else {
        data = MockGroup.findAll
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
   * Return all Mockgroups in Json Format.
   * @return JSON
   */
  def findAll = Action {
    implicit request =>
      val data = MockGroup.findAll
      Ok(Json.toJson(data)).as(JSON)
  }

  /**
   * Return all mockgroups options
   */
  def options = Action {
    implicit request =>
      val data = MockGroup.options
      Ok(Json.toJson(data)).as(JSON)
  }


  /**
   * Display the 'edit form' of a existing Mockgroup.
   *
   * @param id Id of the mockgroup to edit
   */
  def edit(id: Long) = Action {
    MockGroup.findById(id).map {
      mockgroup =>
        Ok(Json.toJson(mockgroup)).as(JSON)
    }.getOrElse(NotFound)
  }


  /**
   * Insert or update a mockgroup.
   */
  def save(id: Long) = Action(parse.json) {
    request =>
      request.body.validate(mockgroupFormat).map {
        mockgroup =>
          try {
            if (id < 0) MockGroup.insert(mockgroup)
            else MockGroup.update(mockgroup)
            Ok(Json.toJson("Succesfully save mockgroup " + id))
          } catch {
            case e : Throwable => { BadRequest("Detected error:" + e.getMessage) }
          }
      }.recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }

  /**
   * Handle mockgroup deletion.
   */
  def delete(id: Long) = Action(parse.tolerantText) { request =>
    val mockgroupOption = MockGroup.findById(id)
    mockgroupOption match {
      case Some(mockgroup) =>
        MockGroup.delete(mockgroup)
        Ok("deleted")
      case None =>
        Ok("failure : Mockgroup doesn't exist")
    }
  }

}
