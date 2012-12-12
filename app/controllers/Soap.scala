package controllers

import play.Logger
import play.api.mvc._
import play.api.libs.iteratee._
import models._
import scala.xml.NodeSeq

object Soap extends Controller {

  def index(environment: String, localTarget: String) = Action(parse.xml) {
    implicit request =>

      Logger.info("Request on environment:" + environment + " localTarget:" + localTarget)

      val sender = request.remoteAddress
      val content = request.body.toString()
      val headers = request.headers.toSimpleMap
      forwardRequest(environment, localTarget, sender, content, headers)
  }

  /**
   * Replay a given request.
   */
  def replay(requestId: Long) = Action {
    val requestData = RequestData.load(requestId)

    val environmentTuple = Environment.options.find { case (k, v) => k == requestData.environmentId.toString }

    if (!environmentTuple.isDefined) {
      val err = "environment with id " + requestData.environmentId + " unknown"
      Logger.error(err)
      BadRequest(err)

    } else {
      val sender = requestData.sender
      val content = requestData.request
      val headers = requestData.requestHeaders
      val environmentName = environmentTuple.get._2
      forwardRequest(environmentName, requestData.localTarget, sender, content, headers)
    }
  }

  private def forwardRequest(environmentName: String, localTarget: String, sender: String, content: String, headers: Map[String, String]): PlainResult = {
    val service = Service.findByLocalTargetAndEnvironmentName(localTarget, environmentName)
    service.map {
      service =>
        // forward the request to the actual destination
        val client = new Client(service, sender, content, headers)
        client.sendRequestAndWaitForResponse

        // forward the response to the client
        SimpleResult(
          header = ResponseHeader(client.response.status, client.response.headers),
          body = Enumerator(client.response.body)) //
          .withHeaders("ProxyVia" -> "soapower").as(XML)

    }.getOrElse {
      val err = "environment " + environmentName + " with localTarget " + localTarget + " unknown"
      Logger.error(err)
      BadRequest(err)
    }
  }

  def printRequest(implicit r: play.api.mvc.RequestHeader) {
    Logger.info("method:" + r)
    Logger.info("headers:" + r.headers)
    //Logger.info("SoapAction:" + r.headers("SOAPACTION"))
    Logger.info("path:" + r.path)
    Logger.info("uri:" + r.uri)
    Logger.info("host:" + r.host)
    Logger.info("rawQueryString:" + r.rawQueryString)
  }

}
