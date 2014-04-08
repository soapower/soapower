package controllers

import play.api.mvc._
import play.Logger
import java.net.URLDecoder
import play.api.libs.iteratee.Enumerator
import models.{Mock, UtilConvert, Service, Client}
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.http.HeaderNames

object Rest extends Controller {

  def index (environment: String, call: String) = Action {
    request  =>
      val sender = request.remoteAddress
      val headers = request.headers.toSimpleMap

      // We retrieve the query and decode each of it's component
      val query = request.queryString.map { case (k,v) =>  URLDecoder.decode(k, "UTF-8") ->  URLDecoder.decode(v.mkString, "UTF-8") }
      val method = request.method
      // We retrieve all the REST services that match the method and the environment
      val localTargets = Service.findRestByMethodAndEnvironmentName(method, environment)

      Logger.debug(localTargets.length +" services found in db")

      // We retrieve the id of the correct
      val serviceId = findService(localTargets, call)
      val service = Service.findById(serviceId)

      service.map {
        service =>
          if (service.useMockGroup) {
            val err = "Soapower doesn't support mock Rest services yet"
            Logger.error(err)
            BadRequest(err)
          } else {
            val remoteTargetWithCall = getRemoteTargetWithCall(call, service.remoteTarget, service.localTarget)
            method match {
              case "GET" =>
                val queryString = URLDecoder.decode(request.rawQueryString, "UTF-8")
                val content = getRequestContent(method, remoteTargetWithCall, queryString)
                forwardRequest(content, query, service, sender, headers, call, remoteTargetWithCall)

              case "POST" =>
                val content = request.body.toString;
                forwardRequest(content, query, service, sender, headers, call, remoteTargetWithCall)

              case "DELETE" =>
                val queryString = URLDecoder.decode(request.rawQueryString, "UTF-8")
                val content = getRequestContent(method, remoteTargetWithCall, queryString)
                forwardRequest(content, query, service, sender, headers, call, remoteTargetWithCall)

              case "PUT" =>
                val content = request.body.toString;
                forwardRequest(content, query, service, sender, headers, call, remoteTargetWithCall)

              case _ =>
                val err = "Not implemented yet"
                Logger.error(err)
                BadRequest(err)
            }
          }
      }.getOrElse {
        val err = "No services with the environment "+environment+" and the HTTP method "+method+" matches the call "+call
        Logger.error(err)
        BadRequest(err)
      }
  }

  def forwardRequest(content: String, query: Map[String, String], service: Service, sender: String, headers: Map[String,String], call: String, correctUrl:String): SimpleResult = {
    val client = new Client(service, sender, content, headers, "get")
    client.sendGetRequestAndWaitForResponse(service.httpMethod, correctUrl, query)

    new Results.Status(client.response.status).apply(client.response.bodyBytes)//.chunked(Enumerator(client.response.bodyBytes).andThen(Enumerator.eof[Array[Byte]]))
      .withHeaders("ProxyVia" -> "soapower")
      .withHeaders(client.response.headers.toArray: _*).as(client.response.contentType)
  }

  /**
   * Find the correct service's id bound to the REST call
   * @param localTargets list of localTargets
   * @param call
   */
  def findService(localTargets: Seq[(Long, String)], call: String): Long =
  {
    for ((serviceId, localTarget) <- localTargets)
    {
      // For each services, check that the call starts with the localTarget
      if(call.startsWith(localTarget))
      {
        return serviceId
      }
    }
    return -1
  }
  /**
   * Get the correct URL for the redirection by parsing the call
   * @param call The call containing the localTarget+"/"+the effective call
   * @param remoteTarget The remoteTarget
   * @return
   */
  def getRemoteTargetWithCall(call: String, remoteTarget: String, localTarget: String): String = {

    val effectiveCall = call.split(localTarget)
    if(effectiveCall.length > 1)
    {
      return remoteTarget+call.split(localTarget)(1)
    }
    else
    {
      return remoteTarget
    }
  }

  /**
   * Get the request content for a GET request (the request content is just the method, the remote target and the potential query
   * @param method the HTTP method
   * @param remoteTargetWithCall the remote target with the correct call
   * @param queryString the query string
   * @return
   */
  def getRequestContent(method: String, remoteTargetWithCall: String, queryString: String): String = {
    var result = method + " " + remoteTargetWithCall
    if(!queryString.isEmpty)
    {
      result += "?"+queryString
    }
    return result
  }
}