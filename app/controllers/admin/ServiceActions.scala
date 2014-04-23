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

object ServiceActions extends Controller {

  implicit val serviceActionFormat = Json.format[ServiceAction]

  /**
   * All ServiceAction attached to groups selected.
   *
   * @return JSON
   */
  def findAll(groups: String) = Action.async {
    val futureDataList = ServiceAction.findInGroups(groups)
    futureDataList.map {
      list =>
        Ok(Json.toJson(Map("data" -> Json.toJson(list))))
    }
  }

  /**
   * Display the 'edit form' of a existing ServiceAction.
   *
   * @param id Id of the serviceAction to edit
   */
  def edit(id: String) = Action.async {
    val futureServiceAction = ServiceAction.findById(BSONObjectID(id))
    futureServiceAction.map {
      serviceAction => Ok(Json.toJson(serviceAction)).as(JSON)
    }
  }

  /**
   * Update a serviceAction.
   */
  def update(id: String) = Action.async(parse.json) {
    request =>
      val idg = BSONObjectID.parse(id).toOption.get
      val json = JsObject(request.body.as[JsObject].fields.filterNot(f => f._1 == "_id")) ++ Json.obj("_id" -> idg)
      try {
        json.validate(ServiceAction.serviceActionFormat).map {
          serviceAction => {
            ServiceAction.update(serviceAction).map {
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
   * Handle serviceAction deletion.
   */
  def delete(id: String) = Action.async(parse.tolerantText) {
    request =>
      ServiceAction.delete(id).map {
        lastError =>
          if (lastError.ok) {
            Ok(id)
          } else {
            BadRequest("Detected error:%s".format(lastError))
          }
      }
  }

  def regenerate() = Action {
    //TODO
    BadRequest("TODO")
    /*
implicit request =>
  RequestData.serviceActionOptions.foreach {
    serviceAction =>
      if (ServiceAction.findByName(serviceAction._1) == None) {
        Logger.debug("ServiceAction not found. Insert in db")
        ServiceAction.insert(new ServiceAction(-1, serviceAction._1, 30000))
      } else {
        Logger.debug("ServiceAction found. Do nothing.")
      }
  }
  Ok(Json.toJson("Success regeneration"))
  */
  }

}
