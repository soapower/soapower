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

object Mocks extends Controller {


  /**
   * All mocks for one mockGroup Id.
   *
   * @return JSON
   */
  def findAll(mockGroupName: String) = Action.async {
    import Mock.mocksFormat
    val futureDataList = Mock.findAll(mockGroupName)
    futureDataList.map {
      mocks => Ok(Json.toJson(mocks.get))
    }
  }

  /**
   * Display the 'edit form' of a existing Mock.
   * @param mockGroupName Name of mockGroup containing the mock
   * @param mockId Id of the mock to edit
   * @return 200 if ok with the mock in Json Format
   */
  def edit(mockGroupName: String, mockId: String) = Action.async {
    val futureMock = Mock.findById(mockGroupName, mockId)
    futureMock.map {
      mock => Ok(Json.toJson(mock.get)).as(JSON)
    }
  }

  /**
   * Insert a mock.
   * @param mockGroupName mockGroup Name wich contains the new mock
   * @return 200 if ok, with id of the new mock
   */
  def create(mockGroupName: String) = Action.async(parse.json) {
    request =>
      val id = BSONObjectID.generate
      val json = request.body.as[JsObject] ++ Json.obj("_id" -> id)
      try {
        json.validate(Mock.mockFormat).map {
          mock => {
            Mock.insert(mock).map {
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
   * Update a mock.
   * @param mockGroupName mockGroup name of the mock to update
   * @param mockId identifier of the mock to update
   * @return 200 if of, with the Id of the updated mock
   */
  def update(mockGroupName: String, mockId: String) = Action.async(parse.json) {
    request =>
      val idg = BSONObjectID.parse(mockId).toOption.get
      val json = JsObject(request.body.as[JsObject].fields.filterNot(f => f._1 == "_id")) ++ Json.obj("_id" -> idg)
      try {
        json.validate(Mock.mockFormat).map {
          mock => {
            Mock.update(mock).map {
              lastError =>
                if (lastError.ok) {
                  Ok(mockId)
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
   * Handle mock deletion.
   */
  def delete(mockGroupName: String, mockId: String) = Action.async(parse.tolerantText) {
    request =>
      Mock.delete(mockGroupName, mockId).map {
        lastError =>
          if (lastError.ok) {
            Ok(mockId)
          } else {
            BadRequest("Detected error:%s".format(lastError))
          }
      }
  }

}
