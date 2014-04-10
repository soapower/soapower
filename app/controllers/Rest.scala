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

      val requestBody = request.body
      // We retrieve the query and decode each of it's component
      val query = request.queryString.map { case (k,v) =>  URLDecoder.decode(k, "UTF-8") ->  URLDecoder.decode(v.mkString, "UTF-8") }
      val httpMethod = request.method
      // We retrieve all the REST services that match the method and the environment
      val localTargets = Service.findRestByMethodAndEnvironmentName(httpMethod, environment)

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
            // Get the correct URL for the redirection by parsing the call.
            val remoteTargetWithCall = getRemoteTargetWithCall(call, service.remoteTarget, service.localTarget)
            var requestContentType= "None"
            var requestContent = ""

            httpMethod match {
              case "GET" | "DELETE" =>
                // We decode the query
                val queryString = URLDecoder.decode(request.rawQueryString, "UTF-8")
                // The content of a GET or DELETE http request is the http method and the remote target call
                requestContent = getRequestContent(httpMethod, remoteTargetWithCall, queryString)

              case "POST" | "PUT" =>
                requestContentType = request.contentType.get
                // The request has a POST or a PUT http method so the request has a body in JSON or XML format
                requestContentType match {
                  case "application/json" =>
                    requestContent = requestBody.asJson.get.toString
                  case "application/xml" | "text/xml" =>
                    requestContent = requestBody.asXml.get.toString
                  case _ =>
                    val err = "Soapower doesn't support request body in this format"
                    Logger.error(err)
                    BadRequest(err)
                }

              case _ =>
                val err = "Soapower doesn't support this HTTP method"
                Logger.error(err)
                BadRequest(err)
            }
            // Forward the request
            forwardRequest(requestContent, query, service, sender, headers, call, remoteTargetWithCall, requestContentType)
          }
      }.getOrElse {
        val err = "No REST services with the environment "+environment+" and the HTTP method "+httpMethod+" matches the call "+call
        Logger.error(err)
        BadRequest(err)
      }
  }

  def replay(requestData: Long) = Action {
    val err = "Replay is not implemented yet for REST services"
    Logger.error(err)
    BadRequest(err)
  }

  def forwardRequest(content: String, query: Map[String, String], service: Service, sender: String, headers: Map[String,String], call: String, correctUrl:String,
                     requestContentType: String): SimpleResult = {
    val client = new Client(service, sender, content, headers, Service.REST, requestContentType)
    client.sendRestRequestAndWaitForResponse(service.httpMethod, correctUrl, query)
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
   * Get the request content for a GET or DELETE request (the request content is just the method with the remote target and the potential query)
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