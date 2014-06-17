package controllers

import play.api.mvc._
import play.Logger
import java.net.URLDecoder
import play.api.libs.iteratee.Enumerator
import models._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.SimpleResult
import org.jboss.netty.handler.codec.http.HttpMethod
import reactivemongo.bson.{BSONObjectID, BSONDocument}

object Rest extends Controller {

  def autoIndex(group: String, environment: String, remoteTarget: String) = Action.async {
    request =>

      Logger.info("Automatic service detection request on group: " + group + " environment:" + environment + " remoteTarget: " + remoteTarget + " httpMethod" + request.method)

      // Extract local target from the remote target
      val localTarget = UtilExtract.extractPathFromURL(remoteTarget)
      val requestHttpMethod = HttpMethod.valueOf(request.method)
      val sender = request.remoteAddress
      val headers = request.headers.toSimpleMap
      val contentType = request.contentType
      val queryString = URLDecoder.decode(request.rawQueryString, "UTF-8")
      val query = request.queryString.map {
        case (k, v) => URLDecoder.decode(k, "UTF-8") -> URLDecoder.decode(v.mkString, "UTF-8")
      }

      if (!localTarget.isDefined) {
        val err = "Invalid remoteTarget:" + remoteTarget
        Logger.error(err)
        BadRequest(err)
      }

      var err: Option[String] = None

      // Search the corresponding possible service
      val optionService = Await.result(Service.findByLocalTargetAndEnvironmentName(Service.REST, localTarget.get, environment, requestHttpMethod), 2.seconds)

      Logger.debug("Option Service:" + optionService)

      if (!optionService.isDefined || optionService.get == null) {
        // If the service doesn't exits
        Logger.info("Create a new service from autorest")
        val description = "This service was automatically generated by soapower"
        val timeoutms = 60000
        val recordContentData = false
        val recordData = false
        val useMockGroup = false
        val typeRequest = Service.REST

        val environmentOption = Await.result(Environment.findByNameAndGroups(environment, group), 2.seconds)
        // Check that the environment exists for the given group
        environmentOption.map {
          environmentReal =>
            Logger.debug("environmentReal : " + environmentReal)
            // The environment exists so the service creation can be performed
            val serviceInsert = new Service(Some(BSONObjectID.generate),
              description,
              typeRequest,
              request.method,
              localTarget.get,
              remoteTarget,
              timeoutms,
              recordContentData,
              recordData,
              useMockGroup,
              None,
              Some(environment))
            // Persist environment to database
            Service.insert(serviceInsert)
        }.getOrElse {
          err = Some("environment " + environment + " with group " + group + " unknown")
        }
      }

      if (err.isDefined) {
        Future(BadRequest(err.get))
      } else {
        // Now the service exists then we have to forward the request
        val service = Service.findByLocalTargetAndEnvironmentName(Service.REST, localTarget.get, environment, requestHttpMethod)
        forwardRequest(service, localTarget.get, environment, sender, headers, request.body, query, contentType, queryString, request.method)
      }

  }

  def index(environment: String, call: String) = Action.async {
    implicit request =>
      val sender = request.remoteAddress
      val headers = request.headers.toSimpleMap

      val requestBody = request.body
      // We retrieve the query and decode each of it's component
      val query = request.queryString.map {
        case (k, v) => URLDecoder.decode(k, "UTF-8") -> URLDecoder.decode(v.mkString, "UTF-8")
      }
      val httpMethod = HttpMethod.valueOf(request.method)
      val contentType = request.contentType
      val queryString = URLDecoder.decode(request.rawQueryString, "UTF-8")
      val service = Service.findByLocalTargetAndEnvironmentName(Service.REST, call, environment, httpMethod)

      forwardRequest(service, call, environment, sender, headers, requestBody, query, contentType, queryString, request.method)
  }

  private def forwardRequest(service: Future[Option[Service]], call: String, environment: String, sender: String, headers: Map[String, String], requestBody: AnyContent, query: Map[String, String], contentType: Option[String], queryString: String, httpMethodForLog: String): Future[SimpleResult] = {

    service.map(svc =>
      if (svc.isDefined && svc.get != null) {
        // Get the correct URL for the redirection by parsing the call.
        val remoteTargetWithCall = getRemoteTargetWithCall(call, svc.get.remoteTarget, svc.get.localTarget)
        // The contentType is set to none by default
        var requestContent = ""

        var err = ""
        var contentTypeExtract = "None"

        svc.get.httpMethod match {
          case "GET" | "DELETE" => {
            // The content of a GET or DELETE http request is the http method and the remote target call
            requestContent = getRequestContent(remoteTargetWithCall, queryString)
          }

          case "POST" | "PUT" => {
            contentTypeExtract = contentType.get
            // The request has a POST or a PUT http method so the request has a body in JSON or XML format
            contentType.get match {
              case "application/json" =>
                requestContent = requestBody.asJson.get.toString
              case "application/xml" | "text/xml" =>
                requestContent = requestBody.asXml.get.toString
              case _ =>
                err = "Soapower doesn't support request body in this format : " + contentType
            }
          }
          case _ => {
            err = "Soapower doesn't support this HTTP method"
          }
        }

        val client = new Client(svc.get, sender, requestContent, headers, Service.REST, contentTypeExtract)

        if (err != "") {
          Logger.error(err)
          BadRequest(err)
        } else if (svc.get.useMockGroup && svc.get.mockGroupId.isDefined) {
          val fmock = Mock.findByMockGroupAndContent(BSONObjectID(svc.get.mockGroupId.get), requestContent)
          val mock = Await.result(fmock, 1.second)
          client.workWithMock(mock)

          val sr = new Results.Status(mock.httpStatus).chunked(Enumerator(mock.response.getBytes()).andThen(Enumerator.eof[Array[Byte]]))
            .withHeaders("ProxyVia" -> "soapower")
            .withHeaders(UtilConvert.headersFromString(mock.httpHeaders).toArray: _*).as(client.requestData.contentType)

          val timeoutFuture = play.api.libs.concurrent.Promise.timeout(sr, mock.timeoutms.milliseconds)
          Await.result(timeoutFuture, 10.second) // 10 seconds (10000 ms) is the maximum allowed.
        } else {
          client.sendRestRequestAndWaitForResponse(HttpMethod.valueOf(svc.get.httpMethod), remoteTargetWithCall, query)
          if (client.response.body == "") client.response.contentType = "text/xml"
          new Results.Status(client.response.status).chunked(Enumerator(client.response.bodyBytes).andThen(Enumerator.eof[Array[Byte]]))
            .withHeaders("ProxyVia" -> "soapower").withHeaders(client.response.headers.toArray: _*).as(client.response.contentType)
        }
      } else {
        val err = "No REST service with the environment " + environment + " and the HTTP method " + httpMethodForLog + " matches the call " + call
        Logger.error(err)
        BadRequest(err)
      }
    )
  }

  def replay(requestId: String) = Action.async {
    implicit request =>
      RequestData.loadRequest(requestId).map {
        tuple => tuple match {
          case Some(doc: BSONDocument) =>
            val requestContentType = doc.getAs[String]("contentType").get
            val sender = doc.getAs[String]("sender").get
            val requestBody = request.body
            import RequestData._
            val headers = doc.getAs[Map[String, String]]("requestHeaders").get
            val environmentName = doc.getAs[String]("environmentName").get
            val service = Service.findById(environmentName, doc.getAs[BSONObjectID]("serviceId").get.stringify)
            val requestCall = doc.getAs[String]("requestCall").get

            val query = request.queryString.map {
              case (k, v) => URLDecoder.decode(k, "UTF-8") -> URLDecoder.decode(v.mkString, "UTF-8")
            }
            val queryString = URLDecoder.decode(request.rawQueryString, "UTF-8")

            Await.result(forwardRequest(service, requestCall, environmentName, sender, headers, requestBody, query, Some(requestContentType), queryString, "fromReplay"), 10.seconds)
          case _ =>
            NotFound("The request " + requestId + "does not exist")
        }
      }
  }

  /**
   * Get the correct URL for the redirection by parsing the call
   * @param call The call containing the localTarget+"/"+the effective call
   * @param remoteTarget The remoteTarget
   *
   * @return
   */
  private def getRemoteTargetWithCall(call: String, remoteTarget: String, localTarget: String): String = {
    val effectiveCall = call.split(localTarget)
    if (effectiveCall.length > 1) {
      return remoteTarget + call.split(localTarget)(1)
    } else {
      return remoteTarget
    }
  }

  /**
   * Get the request content for a GET or DELETE request (the request content is just the remote target and the potential query)
   * @param remoteTargetWithCall the remote target with the correct call
   * @param queryString the query string
   * @return
   */
  private def getRequestContent(remoteTargetWithCall: String, queryString: String): String = {
    var result = remoteTargetWithCall
    if (!queryString.isEmpty) {
      result += "?" + queryString
    }
    return result
  }
}