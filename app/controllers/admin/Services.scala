package controllers.admin

import play.api.mvc._
import models._
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.Future
import play.api.Logger

object Services extends Controller {

  /**
   * All.
   *
   * @return JSON
   */
  def findAll(group: String) = Action.async {
    // TODO Criteria and group
    val futureDataList = Service.findAll
    futureDataList.map {
      list =>
        Ok(Json.toJson(Map("data" -> Json.toJson(list))))
    }
  }

  /**
   * Display the 'edit form' of a existing Service.
   *
   * @param id Id of the service to edit
   */
  def edit(id: String) = Action.async {
    val futureService = Service.findById(id)
    futureService.map {
      service => Ok(Json.toJson(service)).as(JSON)
    }
  }

  /**
   * Insert or update a service.
   */
  def create = Action.async(parse.json) {
    request =>
      val id = BSONObjectID.generate
      val json = request.body.as[JsObject] ++ Json.obj("_id" -> id)
      try {
        json.validate(Service.serviceFormat).map {
          service => {
            Service.insert(service).map {
              lastError =>
                if (lastError.ok) {
                  Ok(id.stringify)
                } else {
                  BadRequest("Detected error on insert :%s".format(lastError))
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
        json.validate(Service.serviceFormat).map {
          service => {
            Service.update(service).map {
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
   * Handle service deletion.
   */
  def delete(id: String) = Action.async(parse.tolerantText) {
    request =>
      Service.delete(id).map {
        lastError =>
          if (lastError.ok) {
            Ok(id)
          } else {
            BadRequest("Detected error:%s".format(lastError))
          }
      }
  }

}
