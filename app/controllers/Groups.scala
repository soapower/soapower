package controllers

import play.api.mvc._

import models._
import play.api.libs.json._
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import play.api.data.Form
import reactivemongo.bson.BSONObjectID
import reactivemongo.bson.{BSONObjectID, BSONDocument}
import play.modules.reactivemongo.json.BSONFormats._
import play.api.Logger

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
   * Retrieve all groups and generate a DataTable's JSON format in order to be displayed in a datatable
   *
   * @return A group JSON datatable data
   */
  def listDatatable = Action.async {
    // TODO Criteria ?
    val futureDataList = Group.findAll
    futureDataList.map {
      list =>
        Ok(Json.toJson(Map("data" -> Json.toJson(list))))
    }
  }

  /**
   * Return all groups options
   */
  def options = Action {
    Ok(Json.toJson(Group.options))
  }

  /**
   * Display the 'edit form' of a existing Group.
   *
   * @param id Id of the group to edit
   */
  def edit(id: String) = Action.async {
    val futureGroup = Group.findById(id)
    futureGroup.map {
      group => Ok(Json.toJson(group)).as(JSON)
    }
  }

  /**
   * Insert a group.
   */
  def create = Action.async(parse.json) {
    request =>
      val id = BSONObjectID.generate
      val json = request.body.as[JsObject] ++ Json.obj("_id" -> id)
      try {
        json.validate(Group.groupFormat).map {
          case group => {
            Group.insert(group).map {
              lastError =>
                if (lastError.ok) {
                  Ok(id.stringify)
                } else {
                  BadRequest("Detected error:%s".format(lastError))
                }
            }
          }
        }.recoverTotal {
          case e => Future.successful(BadRequest("Detected error : " + JsError.toFlatJson(e)))
        }
      } catch {
        case e => Future.successful(BadRequest("Detected error : " + e.getMessage))
      }
  }

  /**
   * Update a group.
   */
  def update(id: String) = Action.async(parse.json) {
    request =>
      val idg = BSONObjectID.parse(id).toOption.get
      val json = JsObject(request.body.as[JsObject].fields.filterNot(f => f._1 == "_id")) ++ Json.obj("_id" -> idg)

      try {
        json.validate(Group.groupFormat).map {
          case group => {
            Group.update(group).map {
              lastError =>
                if (lastError.ok) {
                  Ok(id)
                } else {
                  BadRequest("Detected error:%s".format(lastError))
                }
            }
          }
        }.recoverTotal {
          case e => Future.successful(BadRequest("Detected error validation : " + JsError.toFlatJson(e)))
        }
      } catch {
        case e => Future.successful(BadRequest("Detected error : " + e.getMessage))
      }
  }

  /**
   * Handle group deletion.
   */
  def delete(id: String) = Action.async(parse.tolerantText) {
    request =>
      Group.delete(id).map {
        lastError =>
          if (lastError.ok) {
            Ok(id)
          } else {
            BadRequest("Detected error:%s".format(lastError))
          }
      }
  }

}
