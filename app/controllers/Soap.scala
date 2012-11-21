package controllers

import play.Logger
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._

import models._

object Soap extends Controller {

  def index(environment : String, localTarget: String) = Action(parse.xml) { implicit request => 
    
    val target = if (!localTarget.startsWith("/"))  "/" + localTarget else localTarget

    Logger.info("Request on environment:" + environment + " localTarget:" + localTarget)
    Logger.debug("request:" + request.body.toString)

    Service.findByLocalTargetAndEnvironmentName(target, environment).map { service =>
      val cli = new Client (service, request.body.toString, request.headers.toSimpleMap)
      SimpleResult(
        header = ResponseHeader(200, cli.headers), 
        body = Enumerator(cli.response)
      )
    }.getOrElse{
      val err = "environment " + environment + " with localTarget " + localTarget + " unknown"
      Logger.error(err)
      BadRequest(err)
    }
  
  }

  def printrequest(implicit r: play.api.mvc.RequestHeader) = {
    Logger.info("method:" + r)
    Logger.info("headers:" + r.headers)
    //Logger.info("SoapAction:" + r.headers("SOAPACTION"))
    Logger.info("path:" + r.path)
    Logger.info("uri:" + r.uri)
    Logger.info("host:" + r.host)
    Logger.info("rawQueryString:" + r.rawQueryString)
  }

}