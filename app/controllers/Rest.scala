package controllers

import play.api.mvc._
import play.Logger
import play.api.libs.iteratee.Enumerator
import models.{Mock, UtilConvert, Service, Client}
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.http.HeaderNames

/**
 * Created by glefranc on 28/03/14.
 */
object Rest extends Controller {

  def index (environment: String, call: String) = Action {
    request  =>
      val sender = request.remoteAddress
      val headers = request.headers.toSimpleMap
      val query = request.queryString.map { case (k,v) => k -> v.mkString }
      val method = request.method

      Logger.debug(query.toString)
      val localTargets = Service.findByMethodAndEnvironmentName(method, environment)
      Logger.debug(localTargets.length +" services found in db")
      val serviceId = findService(localTargets, call)
      val service = Service.findById(serviceId)
      service.map {
        service =>
          if (service.useMockGroup) {
            val err = "Soapower doesn't support mock Rest services yet"
            Logger.error(err)
            BadRequest(err)
          } else {
            val correctUrl = getCorrectUrl(call, service.remoteTarget, service.localTarget)
            method match {
              case "GET" =>
                val content = method+" "+correctUrl;
                forwardGetRequest(content, query, service, sender, headers, call, correctUrl)
              /*
              case "DELETE" =>
                val content = method+" "+correctUrl;
                forwardGetRequest(content, query, service, sender, headers, call, correctUrl)
              case "POST" =>
                val content = request.body.toString;
                forwardGetRequest(content, query, service, sender, headers, call, correctUrl)
              case "PUT" =>
                val content = request.body.toString;
                forwardGetRequest(content, query, service, sender, headers, call, correctUrl)
                */
              case _ =>
                val err = "Not implemented yet"
                Logger.error(err)
                BadRequest(err)
            }
          }
      }.getOrElse {
        val err = "No services with the environment "+environment+" matches the call "+call
        Logger.error(err)
        BadRequest(err)
      }
  }

  def forwardGetRequest(content: String, query: Map[String, String], service: Service, sender: String, headers: Map[String,String], call: String, correctUrl:String): SimpleResult = {
    val client = new Client(service, sender, content, headers, "get")
    client.sendRestGetRequestAndWaitForResponse(correctUrl, query)

    new Results.Status(client.response.status).apply(client.response.bodyBytes)//.chunked(Enumerator(client.response.bodyBytes).andThen(Enumerator.eof[Array[Byte]]))
      .withHeaders("ProxyVia" -> "soapower")
      .withHeaders(client.response.headers.toArray: _*).as(client.response.contentType)
  }

  /**
   * Find the correct service bound to the REST call
   * @param localTargets
   * @param call
   */
  def findService(localTargets: Seq[(Long, String)], call: String): Long =
  {
    for ((serviceId, localTarget) <- localTargets)
    {
      if(call.startsWith(localTarget))
      {
        return serviceId
      }
    }
    return -1;
  }
  /**
   * Get the correct URL of the Rest request by parsing the call and add the effectiv call to the remoteTarget
   * @param call
   * @param remoteTarget
   * @return
   */
  def getCorrectUrl(call: String, remoteTarget: String, localTarget: String): String = {
    Logger.debug("call "+call)
    val effectivCall = call.split(localTarget)
    if(effectivCall.length > 1)
    {
      return remoteTarget+call.split(localTarget)(1)
    }
    else
    {
      return remoteTarget
    }
  }
}