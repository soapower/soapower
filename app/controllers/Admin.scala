package controllers

import play.api.mvc._
import models._
import play.api.libs.iteratee._
import play.api.http._


object Admin extends Controller {

  val Home = Redirect(routes.Admin.index())

  def index = Action { implicit request =>
      Ok(views.html.admin.index())
  }

  def uploadConfiguration = Action(parse.multipartFormData) { request =>
    request.body.file("services").map { services =>
      import scala.io._
      var err = ""
      Source.fromFile(services.ref.file).getLines().foreach { line =>
        try {
          Service.upload(line)
        } catch {
          case e: Exception => {
            err += e.getMessage
          }
        }
      }
      if (err.size > 0) {
        Home.flashing("warning" -> err)
      } else {
        Home.flashing("success" -> "Configuration Upload")
      }

    }.getOrElse {
      Home.flashing("error" -> "Failed to upload configuration")
    }
  }

  def downloadConfiguration = Action {
    var content = ""
    Service.csvTitle.toList.sortBy(_._2).foreach{ case (k,v)  => content += k  + ";"}
    content = content.dropRight(1) + "\n" // delete last ; and add new line
    Service.fetchCsv().foreach { s => content += s }
    val fileContent: Enumerator[String] = Enumerator(content)
    SimpleResult(
      header = ResponseHeader(play.api.http.Status.OK),
      body = fileContent
    ).withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=services.csv")).as(XML)
  }

  def deleteAllRequestData = Action {
    RequestData.deleteAll()
    Home.flashing("success" -> "Request Data deleted")
  }
}
