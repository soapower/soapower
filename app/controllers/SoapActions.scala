package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import anorm._

import models._

object SoapActions extends Controller {

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.SoapActions.list(0, 2, ""))

  /**
   * Describe the soapAction form (used in both edit and create screens).
   */
  val soapActionForm = Form(
    mapping(
      "id" -> ignored(NotAssigned: Pk[Long]),
      "name" -> text,
      "thresholdms" -> longNumber)(SoapAction.apply)(SoapAction.unapply))

  /**
   * Display the paginated list of soapActions.
   *
   * @param page Current page number (starts from 0)
   * @param orderBy Column to be sorted
   * @param filter Filter applied on soap Action
   */
  def list(page: Int, orderBy: Int, filter: String) = Action { implicit request =>
    Ok(views.html.soapActions.list(
      SoapAction.list(page = page, orderBy = orderBy, filter = ("%" + filter + "%")),
      orderBy, filter))
  }

  /**
   * Display the 'edit form' of a existing SoapAction.
   *
   * @param id Id of the soapAction to edit
   */
  def edit(id: Long) = Action {
    SoapAction.findById(id).map { soapAction =>
      Ok(views.html.soapActions.editForm(id, soapActionForm.fill(soapAction)))
    }.getOrElse(NotFound)
  }

  /**
   * Handle the 'edit form' submission
   *
   * @param id Id of the soapAction to edit
   */
  def update(id: Long) = Action { implicit request =>
    soapActionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.soapActions.editForm(id, formWithErrors)),
      soapAction => {
        SoapAction.update(id, soapAction)
        Home.flashing("success" -> "Threshold has been updated")
      })
  }

  def regenerate = Action { implicit request =>
          Home.flashing("success" -> "Success regeneration")
  }
}
