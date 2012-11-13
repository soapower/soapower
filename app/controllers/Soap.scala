package controllers

import play.api._
import play.api.mvc._

object Soap extends Controller {
  
  def index(url: String) = Action { implicit request => 
    printrequest
    println("request:" + request.body)
    Ok("OK")
  }


  def printrequest(implicit r: play.api.mvc.RequestHeader) = {
    println("method:" + r)
    println("headers:" + r.headers)
    println("SoapAction:" + r.headers("SOAPACTION"))
    println("path:" + r.path)
    println("uri:" + r.uri)
    println("host:" + r.host)
    println("rawQueryString:" + r.rawQueryString)
  }
  
}