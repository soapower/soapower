package controllers.admin

import play.api.mvc._
import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import reactivemongo.bson.BSONObjectID
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import play.modules.reactivemongo.json.BSONFormats._
import play.api.Logger

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

  /**
   * List to Datable table.
   *
   * @return JSON
   */
  def findAll(group: String) = Action.async {
    // TODO Criteria and group
    val futureDataList = Environment.findAll
    futureDataList.map {
      list =>
        Ok(Json.toJson(Map("data" -> Json.toJson(list))))
    }
  }

  /*  def listDatatable(group: String) = Action {
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
    }*/

  /**
   * Return all Environments in Json Format
   * @return JSON
   */
  def options(group: String) = Action {
    Ok(Json.toJson(Environment.options))
  }

  /*
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
  */

  /**
   * Display the 'edit form' of a existing Environment.
   *
   * @param id Id of the environment to edit
   */
  def edit(id: String) = Action.async {
    val futureEnvironment = Environment.findById(id)
    futureEnvironment.map {
      environment => Ok(Json.toJson(environment)).as(JSON)
    }
  }

  /**
   * Insert or update a environment.
   */
  def create = Action.async(parse.json) {
    request =>
      val id = BSONObjectID.generate
      val json = request.body.as[JsObject] ++ Json.obj("_id" -> id)
      try {
        json.validate(Environment.environmentFormat).map {
          environment => {
            Environment.insert(environment).map {
              lastError =>
                if (lastError.ok) {
                  Ok(id.stringify)
                } else {
                  BadRequest("Detected error on insertupdate :%s".format(lastError))
                }
            }
          }
        }.recoverTotal {
          case e => Future.successful(BadRequest("Detected error on validation : " + JsError.toFlatJson(e)))
        }
      } catch {
        case e: Throwable => {
          Logger.error("Error:", e)
          Future.successful(BadRequest("Internal error : " + e.getMessage))
        }
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
        json.validate(Environment.environmentFormat).map {
          environment => {
            Environment.update(environment).map {
              lastError =>
                if (lastError.ok) {
                  Ok(id)
                } else {
                  BadRequest("Detected error on update :%s".format(lastError))
                }
            }
          }
        }.recoverTotal {
          case e => Future.successful(BadRequest("Detected error on validation : " + JsError.toFlatJson(e)))
        }
      } catch {
        case e: Throwable => {
          Logger.error("Error:", e)
          Future.successful(BadRequest("Internal error : " + e.getMessage))
        }
      }
  }

  /**
   * Handle environment deletion.
   */
  def delete(id: String) = Action.async(parse.tolerantText) {
    request =>
      Environment.delete(id).map {
        lastError =>
          if (lastError.ok) {
            Ok(id)
          } else {
            BadRequest("Detected error:%s".format(lastError))
          }
      }
  }

  def findAllGroups() = Action.async {
    Environment.findAllGroups().map {
      list =>
        Ok(Json.toJson(list))
    }
  }

}

