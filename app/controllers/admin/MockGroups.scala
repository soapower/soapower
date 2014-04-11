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

object MockGroups extends Controller {

  // use by Json : from scala to json
  private implicit object MockGroupsOptionsDataWrites extends Writes[(String, String)] {
    def writes(data: (String, String)): JsValue = {
      JsObject(
        List(
          "id" -> JsString(data._1),
          "name" -> JsString(data._2)
        ))
    }
  }

  /**
   * All.
   *
   * @return JSON
   */
  def findAll(group: String) = Action.async {
    // TODO Criteria and group
    val futureDataList = MockGroup.findAll
    futureDataList.map {
      list =>
        Ok(Json.toJson(Map("data" -> Json.toJson(list))))
    }
  }

  /**
   * Display the 'edit form' of a existing MockGroup.
   *
   * @param id Id of the mockGroup to edit
   */
  def edit(id: String) = Action.async {
    Logger.debug("EDIT id:" + id)
    val futureMockGroup = MockGroup.findById(id)
    futureMockGroup.map {
      mockGroup => Ok(Json.toJson(mockGroup)).as(JSON)
    }
  }

  /**
   * Insert or update a mockGroup.
   */
  def create = Action.async(parse.json) {
    request =>
      val id = BSONObjectID.generate
      val json = request.body.as[JsObject] ++ Json.obj("_id" -> id)
      try {
        json.validate(MockGroup.mockGroupFormat).map {
          mockGroup => {
            MockGroup.insert(mockGroup).map {
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
        json.validate(MockGroup.mockGroupFormat).map {
          mockGroup => {
            MockGroup.update(mockGroup).map {
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
   * Handle mockGroup deletion.
   */
  def delete(id: String) = Action.async(parse.tolerantText) {
    request =>
      MockGroup.delete(id).map {
        lastError =>
          if (lastError.ok) {
            Ok(id)
          } else {
            BadRequest("Detected error:%s".format(lastError))
          }
      }
  }

}

