package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import anorm._

import models._

object Environments extends Controller {

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Environments.list(0, 2, ""))

  /**
   * Describe the environment form (used in both edit and create screens).
   */
  val environmentForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "name" -> nonEmptyText,
      "hourRecordXmlDataMin" -> number(min=0, max=23),
      "hourRecordXmlDataMax" -> number(min=0, max=24),
      "nbDayKeepXmlData" -> number(min=0, max=10),
      "nbDayKeepAllData" -> number(min=11, max=50)) (Environment.apply)(Environment.unapply))

  /**
   * Display the paginated list of environments.
   *
   * @param page Current page number (starts from 0)
   * @param orderBy Column to be sorted
   * @param filter Filter applied on soap Action
   */
  def list(page: Int, orderBy: Int, filter: String) = Action { implicit request =>
    Ok(views.html.environments.list(
      Environment.list(page = page, orderBy = orderBy, filter = ("%" + filter + "%")),
      orderBy, filter))
  }

  /**
   * Display the 'edit form' of a existing Environment.
   *
   * @param id Id of the environment to edit
   */
  def edit(id: Long) = Action {
    Environment.findById(id).map { environment =>
      Ok(views.html.environments.editForm(id, environmentForm.fill(environment)))
    }.getOrElse(NotFound)
  }

  /**
   * Handle the 'edit form' submission
   *
   * @param id Id of the environment to edit
   */
  def update(id: Long) = Action { implicit request =>
    environmentForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.environments.editForm(id, formWithErrors)),
      environment => {
        Environment.update(id, environment)
        Home.flashing("success" -> "Environment %s has been updated".format(environment.name))
      })
  }

  /**
   * Display the 'new environment form'.
   */
  def create = Action {
    Ok(views.html.environments.createForm(environmentForm.fill(new Environment(NotAssigned, "", 6, 22, 5))))
  }

  /**
   * Handle the 'new environment form' submission.
   */
  def save = Action { implicit request =>
    environmentForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.environments.createForm(formWithErrors)),
      environment => {
        Environment.insert(environment)
        Home.flashing("success" -> "Environment %s has been created".format(environment.name))
      })
  }

  /**
   * Handle environment deletion.
   */
  def delete(id: Long) = Action {
    Environment.delete(id)
    Home.flashing("success" -> "Environment has been deleted")
  }

}
