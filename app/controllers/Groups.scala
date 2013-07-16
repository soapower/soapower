package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import anorm._

import models._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

/**
 * This controller handle all operations related to groups
 */

object Groups extends Controller {

	// use by Json : from scala to json
	private implicit object StatsDataWrites extends Writes[(String, String)] {
		def writes(groupToWrite: (String, String)): JsValue = {
				JsObject(
						List(
								"0" -> JsString(groupToWrite._1),
								"1" -> JsString("<a href=\"groups/"+groupToWrite._2+"\"><i class=\"icon-edit\"></i> Edit</a>")
								))
		}
	}


	/**
	 * Group format
	 */
	implicit val groupFormat = Json.format[Group]

	/**
	 * Retrieve all groups and generate a DataTable's JSON format in order to be displayed in a datatable
	 *
	 * @return A group JSON datatable data
	 */
	def listDatatable = Action { implicit request =>
		val data = Group.allGroups
		Ok(Json.toJson(Map(
					"iTotalRecords" -> Json.toJson(data.size),
					"iTotalDisplayRecords" -> Json.toJson(data.size),
					"aaData" -> Json.toJson(data)
					))).as(JSON)
	}





	/**
	 * Return all Groups in Json Format
	 * @return JSON
	 */
	def findAll = Action { implicit request =>
		val data = Group.allGroups
		Ok(Json.toJson(data)).as(JSON)
	}

	/**
	 * Return all Environments in Json Format
	 * @return JSON
	 */
	def options = Action { implicit request =>
	val data = Group.options
	Ok(Json.toJson(data)).as(JSON)
	}





	/**
	 * Insert or update a environment.
	 */
	def save(id: Long) = Action(parse.json) { request =>
	request.body.validate(groupFormat).map { group =>
	if (id < 0) Group.insert(group)
	else Group.update(group)
	Ok(Json.toJson("Succesfully save group."))
	}.recoverTotal{
		e => BadRequest("Detected error:"+ JsError.toFlatJson(e))
	}
	}




	/**
	 * Handle environment deletion.
	 */
	def delete(id: Long) = Action {
		val groupOption = Group.findById(id)
				groupOption match{
				case Some(group) =>
				Group.delete(group)
				Ok("deleted");
				case None =>
				Ok("failure : Group doesn't exist")
		}

	}



}
