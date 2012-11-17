package controllers

import play.api._
import play.api.mvc._

import models._

object Soap extends Controller {
  
  def index(environment : String, localTarget: String) = Action { implicit request => 
    
    //printrequest
    val target = if (!localTarget.startsWith("/"))  "/" + localTarget else localTarget

    Logger.info("request:" + request.body)
    Logger.info("environment:" + environment)
    Logger.info("localTarget: " + target)


    Service.findByLocalTargetAndEnvironmentName(target, environment).map { service =>
      val cli = new Client 
      Ok(cli.send(service.remoteTarget, request.body.toString, request.headers.toSimpleMap).toString)
      //Ok("processing")
    }.getOrElse(BadRequest("environment and localTarget unknown"))

  }

  def process(service: Service) = {

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
    //cli.send(host, port, path, req, r.headers.toSimpleMap) 
    val remoteTarget = "http://localhost:9000/soap/testlog"
    cli.send(remoteTarget, req, r.headers.toSimpleMap) 
  } 

}