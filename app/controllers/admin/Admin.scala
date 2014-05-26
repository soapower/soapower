package controllers.admin

import play.api.mvc._
import models._
import play.api.libs.iteratee._
import play.api.http._
import java.util.{Calendar, GregorianCalendar, TimeZone, Date}
import play.api._
import play.api.libs.json.{JsError, Json}
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import java.text.SimpleDateFormat

object Admin extends Controller {

  case class AdminForm(environmentName: String, minDate: String, maxDate: String, typeAction: String)

  def toJson(code: String, text: String) = Json.obj("code" -> code, "text" -> text)

  def uploadConfiguration = Action(parse.multipartFormData) {
    request =>
      request.body.file("fileUploaded").map {
        fileUploaded =>
          import scala.io._
          var err = ""
          Source.fromFile(fileUploaded.ref.file).getLines().foreach {
            line =>
              try {
                if (line.startsWith(ServiceAction.csvKey)) ServiceAction.upload(line)
                else if (line.startsWith(Service.csvKey)) Service.upload(line)
                else if (line.startsWith(Environment.csvKey)) Environment.upload(line)
                else if (line.startsWith(RequestData.csvKey)) RequestData.upload(line)
                else if (line.startsWith(MockGroup.csvKey)) MockGroup.upload(line)
              } catch {
                case e: Exception => err += e.getMessage
              }
          }
          if (err.size > 0) {
            Ok(toJson("warning", "Warning ! Configuration uploaded partially. See Warn Logs")).as(JSON)
          } else {
            Ok(toJson("success", "Success ! Configuration Uploaded")).as(JSON)
          }
      }.getOrElse {
        Ok(toJson("warning", "Warning ! Failed to upload configuration")).as(JSON)
      }
  }

  def downloadConfiguration = Action.async {
    // Title
    var content = "#for key " + Environment.csvKey + "\n"
    Environment.csvTitle.toList.sortBy(_._2).foreach {
      case (k, v) => content += k + ";"
    }
    content = content.dropRight(1) + "\n" // delete last ; and add new line
    content += "#for key " + ServiceAction.csvKey + "\n"
    ServiceAction.csvTitle.toList.sortBy(_._2).foreach {
      case (k, v) => content += k + ";"
    }
    content = content.dropRight(1) + "\n"
    content += "#for key " + MockGroup.csvKey + "\n"
    MockGroup.csvTitle.toList.sortBy(_._2).foreach {
      case (k, v) => content += k + ";"
    }
    content = content.dropRight(1) + "\n"
    content += "#for key " + Service.csvKey + "\n"
    Service.csvTitle.toList.sortBy(_._2).foreach {
      case (k, v) => content += k + ";"
    }
    content = content.dropRight(1) + "\n"

    def combine(csv: List[Object]) = csv.foreach(s => content += s)

    def f: Future[Unit] = {
      for {
        environments <- Environment.fetchCsv()
        mockGroups <- MockGroup.fetchCsv()
        services <- Service.fetchCsv()
        servicesActions <- ServiceAction.fetchCsv()
      } yield combine(environments ++ servicesActions ++ mockGroups ++ services)
    }

    // result as a file
    f map {
      case _ =>
        val fileContent: Enumerator[Array[Byte]] = Enumerator(content.getBytes)
        SimpleResult(
          header = ResponseHeader(play.api.http.Status.OK),
          body = fileContent
        ).withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=configuration.csv")).as(BINARY)
    }
  }

  def downloadRequestDataStatsEntries = Action.async {
    // Title
    var content = "#for key " + Stat.csvKey + "\n"
    Stat.csvTitle.toList.sortBy(_._2).foreach {
      case (k, v) => content += k + ";"
    }
    content = content.dropRight(1) + "\n" // delete last ; and add new line

    // data
    Stat.fetchCsv().map {
      list =>
        list.foreach {
          s =>
            content += s
        }

        val fileContent: Enumerator[Array[Byte]] = Enumerator(content.getBytes)
        SimpleResult(
          header = ResponseHeader(play.api.http.Status.OK),
          body = fileContent
        ).withHeaders((HeaderNames.CONTENT_DISPOSITION, "attachment; filename=requestDataStatsEntries.csv")).as(BINARY)

    }
  }

  implicit val adminFormat = Json.format[AdminForm]

  def delete = Action(parse.json) {

    request =>
      request.body.validate(adminFormat).map {
        adminForm =>
          val format = new SimpleDateFormat("yyyy-MM-dd HH:mm")
          val correctMinDate = format.parse(adminForm.minDate)
          val correctMaxDate = format.parse(adminForm.maxDate)

          adminForm.typeAction match {
            case "xml-data" =>
              RequestData.deleteRequestResponse(adminForm.environmentName, correctMinDate, correctMaxDate, "Admin Soapower")
            case "all-data" =>
              RequestData.delete(adminForm.environmentName, correctMinDate, correctMaxDate)
          }


          Ok(toJson("success", "Success deleting data")).as(JSON)
      }.recoverTotal {
        e => Ok(toJson("error", "Detected error:" + JsError.toFlatJson(e))).as(JSON)
      }
  }
}
