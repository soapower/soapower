package controllers

import play.api._
import play.api.mvc._

import models._

object Soap extends Controller {
  
  def index(environment : String, localTarget: String) = Action { implicit request => 
    
    //printrequest
    Logger.info("request:" + request.body)

    Service.findByLocalTargetAndEnvironmentName(localTarget, environment).map { service =>
      Ok("environment:" + environment + " localTarget:" + localTarget + " headers" + request.headers)  
    }
    BadRequest("environment and localTarget unknown")
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

  def test = Action { implicit request => 
      sendWithClient
      Ok("Test ended, look at log")
  }

  def testLog = Action { implicit request =>
    printrequest
    Logger.info("request:" + request.body)
    Ok("testLog OK")
  }

  def sendWithClient ( implicit r: play.api.mvc.RequestHeader) = {
    val host = "localhost" 
    val port = 9000
    val path = "/soap/testlog" 
    val req = "content of request"
    val cli = new Client 
    Logger.info("##### Test send: request:" + req) 
    cli.send(host, port, path, req, r.headers.toSimpleMap) 
  } 

}