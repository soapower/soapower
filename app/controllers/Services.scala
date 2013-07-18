package controllers

import play.api.mvc._
import models._
import play.api.libs.json._

object Services extends Controller {


  private implicit object ServicesDataWrites extends Writes[(Service, Environment)] {
    def writes(data: (Service, Environment)): JsValue = {
      JsObject(
        List(
          "id" -> JsString(data._1.id.toString),
          "description" -> JsString(data._1.description),
          "env" -> JsString(data._2.name),
          "localTarget" -> JsString("/soap/" + data._2.name + "/" + data._1.localTarget),
          "remoteTarget" -> JsString(data._1.remoteTarget),
          "timeoutInMs" -> JsNumber(data._1.timeoutms),
          "recordXmlData" -> JsBoolean(data._1.recordXmlData),
          "recordData" -> JsBoolean(data._1.recordData)
        ))
    }
  }

  implicit val serviceFormat = Json.format[Service]

  /**
   * List to Datable table.
   *
   * @return JSON
   */
  def listDatatable(group : String) = Action {
    implicit request =>

     var data : List[(Service, Environment)] = null.asInstanceOf[ List[(Service, Environment)] ]
     println(group)
      if(group != "all"){

        data = Service.list(group)
      } else{
        data = Service.list
      }
      Ok(Json.toJson(Map(
        "iTotalRecords" -> Json.toJson(data.size),
        "iTotalDisplayRecords" -> Json.toJson(data.size),
        "data" -> Json.toJson(data)
      ))).as(JSON)
  }

  /**
   * Display the 'edit form' of a existing Service.
   *
   * @param id Id of the service to edit
   */
  def edit(id: Long) = Action {
    Service.findById(id).map {
      service =>
        Ok(Json.toJson(service)).as(JSON)
    }.getOrElse(NotFound)
  }

  /**
   * Insert or update a service.
   */
  def save(id: Long) = Action(parse.json) { request =>
    request.body.validate(serviceFormat).map { service =>
      if (id < 0) Service.insert(service)
      else Service.update(service)
      Ok(Json.toJson("Succesfully save service."))
    }.recoverTotal{
      e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
    }
  }

  /**
   * Handle service deletion.
   */
  def delete(id: Long) = Action {
    Service.delete(id)
    Ok("deleted");
  }

}
