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
   * All services for one environment Id.
   *
   * @return JSON
   */
  def findAll(environmentName: String) = Action.async {
    import Service.servicesFormat
    val futureDataList = Service.findAll(environmentName)
    futureDataList.map {
      services => Ok(Json.toJson(services.get))
    }
  }

  /**
   * Display the 'edit form' of a existing Service.
   * @param environmentName Name of environment containing the service
   * @param serviceId Id of the service to edit
   * @return 200 if ok with the service in Json Format
   */
  def edit(environmentName: String, serviceId: String) = Action.async {
    val futureService = Service.findById(environmentName, serviceId)
    futureService.map {
      service => {
        Logger.debug("Service:" + service)
        Ok(Json.toJson(service.get)).as(JSON)
      }
    }
  }

  /**
   * Insert or update a service.
   * @return
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
   * @param environmentName
   * @param serviceId
   * @return
   */
  def update(environmentName: String, serviceId: String) = Action.async(parse.json) {
    request =>
      val idg = BSONObjectID.parse(serviceId).toOption.get
      val json = JsObject(request.body.as[JsObject].fields.filterNot(f => f._1 == "_id")) ++ Json.obj("_id" -> idg)
      try {
        json.validate(Service.serviceFormat).map {
          service => {
            Service.update(service).map {
              lastError =>
                if (lastError.ok) {
                  Ok(serviceId)
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
  def delete(environmentId: String, serviceId: String) = Action.async(parse.tolerantText) {
    request =>
      ???
      //TODO
      Service.delete(serviceId).map {
        lastError =>
          if (lastError.ok) {
            Ok(serviceId)
          } else {
            BadRequest("Detected error:%s".format(lastError))
          }
      }
  }

}
