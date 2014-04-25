package controllers

import play.api.mvc._
import play.api.libs.json.Json


object Index extends Controller {

  /**
   * Return Build info in Json Format
   * @return JSON
   */
  def getBuildInfo = Action {
    Ok(Json.obj(
      "name" -> soapower.build.info.BuildInfo.name,
      "projectName" -> soapower.build.info.BuildInfo.projectName,
      "version" -> soapower.build.info.BuildInfo.version,
      "versionDoc" -> soapower.build.info.BuildInfo.versionDoc,
      "scalaVersion" -> soapower.build.info.BuildInfo.scalaVersion,
      "sbtVersion" -> soapower.build.info.BuildInfo.sbtVersion
    )).as(JSON)
  }

}
