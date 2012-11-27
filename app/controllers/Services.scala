package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import anorm._

import models._

object Services extends Controller {

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Services.list(0, 2, ""))

  /**
   * Describe the service form (used in both edit and create screens).
   */
  val serviceForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "description" -> nonEmptyText,
      "localTarget" -> nonEmptyText,
      "remoteTarget" -> nonEmptyText,
      "timeoutms" -> longNumber,
      "user" -> optional(text),
      "password" -> optional(text),
      "environment" -> longNumber)(Service.apply)(Service.unapply))

  def index = Action {
    Ok("Index Services")
  }

  /**
   * Display the paginated list of services.
   *
   * @param page Current page number (starts from 0)
   * @param orderBy Column to be sorted
   * @param filter Filter applied on soap Action
   */
  def list(page: Int, orderBy: Int, filter: String) = Action { implicit request =>
    Ok(views.html.services.list(
      Service.list(page = page, orderBy = orderBy, filter = ("%" + filter + "%")),
      orderBy, filter))
  }

  /**
   * Display the 'edit form' of a existing Service.
   *
   * @param id Id of the service to edit
   */
  def edit(id: Long) = Action {
    Service.findById(id).map { service =>
      Ok(views.html.services.editForm(id, serviceForm.fill(service), Environment.options))
    }.getOrElse(NotFound)
  }

  /**
   * Handle the 'edit form' submission
   *
   * @param id Id of the service to edit
   */
  def update(id: Long) = Action { implicit request =>
    serviceForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.services.editForm(id, formWithErrors, Environment.options)),
      service => {
        Service.update(id, service)
        Home.flashing("success" -> "Service %s has been updated".format(service.description))
      })
  }

  /**
   * Display the 'new service form'.
   */
  def create = Action {
    Ok(views.html.services.createForm(serviceForm, Environment.options))
  }

  /**
   * Handle the 'new service form' submission.
   */
  def save = Action { implicit request =>
    serviceForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.services.createForm(formWithErrors, Environment.options)),
      service => {
        Service.insert(service)
        Home.flashing("success" -> "Service %s has been created".format(service.description))
      })
  }

  /**
   * Handle service deletion.
   */
  def delete(id: Long) = Action {
    Service.delete(id)
    Home.flashing("success" -> "Service has been deleted")
  }

}
