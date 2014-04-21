package models

import com.ning.http.client.FluentCaseInsensitiveStringsMap
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent._
import play.core.utils.CaseInsensitiveOrdered
import play.Logger
import collection.immutable.TreeMap
import play.api.http._
import java.io.StringWriter
import java.io.PrintWriter
import play.api.Play.current
import com.ning.http.client.Realm.AuthScheme

object Client {
  private val DEFAULT_NO_SOAPACTION = "Soapower_NoSoapAction"
  private val nbRequest = new java.util.concurrent.atomic.AtomicLong

  var lock: AnyRef = new Object()

  def getNbRequest: Long = nbRequest.get

  def processQueue(requestData: RequestData) {
    val writeStartTime = System.currentTimeMillis()
    scala.concurrent.Future {
      lock.synchronized {
        nbRequest.addAndGet(1)
      }
    }.map {
      result =>
        Logger.debug("Request Data store to queue in " + (System.currentTimeMillis() - writeStartTime) + " ms")
    }
  }

  def extractSoapAction(headers: Map[String, String]): String = {
    val soapActionHeader = headers.get("SOAPAction")
    soapActionHeader match {
      case Some(soapAction) =>
        // drop apostrophes if present
        if (soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
          soapAction.drop(1).dropRight(1)
        } else {
          soapAction
        }
      case None => DEFAULT_NO_SOAPACTION
    }
  }
}

class Client(service: Service, sender: String, content: String, headers: Map[String, String], typeRequest: String, requestContentType: String) {

  val requestData = {
    var serviceAction = ""

    if ((typeRequest == Service.SOAP) && (Client.extractSoapAction(headers) != Client.DEFAULT_NO_SOAPACTION)) {
      serviceAction = Client.extractSoapAction(headers)
    } else {
      serviceAction = service.httpMethod.toUpperCase + " " + service.localTarget
    }

    Logger.debug("service:" + service)

    new RequestData(sender, serviceAction, service.environmentName.get, service._id.get, requestContentType)
  }

  val environment = Environment.findByName(service.environmentName.get)
  var response: ClientResponse = null

  private var futureResponse: Future[Response] = null
  private var requestTimeInMillis: Long = -1

  def workWithMock(mock: Mock) {
    requestData.isMock = true
    requestData.timeInMillis = mock.timeoutms
    requestData.status = mock.httpStatus

    requestData.requestHeaders = headers
    requestData.response = checkNullOrEmpty(mock.response)
    requestData.responseHeaders = UtilConvert.headersFromString(mock.httpHeaders)
    if (requestData.contentType == "None") {
      val mockResponseType = requestData.responseHeaders.get("Content-Type")
      mockResponseType match {
        case Some(content) =>
          requestData.contentType = requestData.responseHeaders.get("Content-Type").get
        case _ =>
          requestData.contentType = "text/html"
      }
      requestData.contentType = requestData.responseHeaders.get("Content-Type").get
    }
    saveData(content)
    Logger.debug("End workWithMock")
  }

  /**
   * Send a request to a REST service
   * @param correctUrl
   * @param query
   */
  def sendRestRequestAndWaitForResponse(method: String, correctUrl: String, query: Map[String, String]) {
    requestTimeInMillis = System.currentTimeMillis
    // Keep the call in the request data for replay functionality
    requestData.requestCall = correctUrl
    // prepare request
    var wsRequestHolder = WS.url(correctUrl).withRequestTimeout(service.timeoutms.toInt)
    wsRequestHolder = wsRequestHolder.withQueryString(query.toList: _*)
    wsRequestHolder = wsRequestHolder.withHeaders((HeaderNames.X_FORWARDED_FOR -> sender))

    // add headers
    def filteredHeaders = headers.filterNot {
      _._1 == HeaderNames.TRANSFER_ENCODING
    }
    wsRequestHolder = wsRequestHolder.withHeaders(filteredHeaders.toArray: _*)

    try {
      // perform request
      method match {
        case Service.GET =>
          futureResponse = wsRequestHolder.get()
        case Service.POST =>
          wsRequestHolder = wsRequestHolder.withHeaders((HeaderNames.CONTENT_LENGTH -> content.getBytes.size.toString))
          futureResponse = wsRequestHolder.post(content)
        case Service.DELETE =>
          futureResponse = wsRequestHolder.delete()
        case Service.PUT =>
          wsRequestHolder = wsRequestHolder.withHeaders((HeaderNames.CONTENT_LENGTH -> content.getBytes.size.toString))
          futureResponse = wsRequestHolder.put(content)
      }
      // wait for the response
      waitForResponse(headers)

    } catch {
      case e: Throwable =>
        requestData.contentType match {
          case "application/xml" | "text/xml" =>
            processError(method, "xml", e)
          case "application/json" =>
            processError(method, "json", e)
          case _ =>
            processError(method, "text", e)
        }

    }
    // save the request and response data to DB
    saveData(content)
  }

  /**
   * Send a request to a SOAP service
   */
  def sendSoapRequestAndWaitForResponse() {
    if (Logger.isDebugEnabled) {
      Logger.debug("RemoteTarget " + service.remoteTarget)
    }

    requestTimeInMillis = System.currentTimeMillis

    // prepare request
    var wsRequestHolder = WS.url(service.remoteTarget).withRequestTimeout(service.timeoutms.toInt)
    wsRequestHolder = wsRequestHolder.withHeaders((HeaderNames.X_FORWARDED_FOR -> sender))
    // add headers
    def filteredHeaders = headers.filterNot {
      _._1 == HeaderNames.TRANSFER_ENCODING
    }
    wsRequestHolder = wsRequestHolder.withHeaders(filteredHeaders.toArray: _*)
    wsRequestHolder = wsRequestHolder.withHeaders((HeaderNames.CONTENT_LENGTH -> content.getBytes.size.toString))

    try {
      // perform request
      futureResponse = wsRequestHolder.post(content)
      // wait for the response
      waitForResponse(headers)

    } catch {

      case e: Throwable =>
        processError("post", "xml", e)
    }
    // save the request and response data to DB
    saveData(content)
  }

  private def waitForResponse(headers: Map[String, String]) = {
    try {
      val wsResponse: Response = Await.result(futureResponse, service.timeoutms.millis * 1000000)
      response = new ClientResponse(wsResponse, (System.currentTimeMillis - requestTimeInMillis))
      requestData.timeInMillis = response.responseTimeInMillis
      requestData.status = response.status
      Client.processQueue(requestData)
      requestData.requestHeaders = headers

      if (requestData.contentType == "None") {
        // If the request content type is "None", the http method of the request was GET or DELETE,
        // the contentType is set to the response contentType (json, xml or text)
        requestData.contentType = response.contentType
      }

      requestData.response = checkNullOrEmpty(response.body)
      requestData.responseBytes = response.bodyBytes

      requestData.responseHeaders = response.headers

      if (Logger.isDebugEnabled) {
        Logger.debug("Response in " + response.responseTimeInMillis + " ms")
      }
    } catch {
      case e: Throwable =>
        requestData.contentType match {
          case "application/xml" | "text/xml" =>
            processError("waitForResponse", "xml", e)
          case "application/json" =>
            processError("waitForResponse", "json", e)
          case _ =>
            processError("waitForResponse", "text", e)
        }
    }
  }

  /**
   * If content is null or empty, return "[null or empty]"
   * @param content a string
   * @return [null or empty] or the content if not null (or empty!)
   */
  private def checkNullOrEmpty(content: String): String = {
    if (content == null || content.isEmpty) "[null or empty]" else content
  }

  private def saveData(content: String) = {
    try {
      // asynchronously writes data to the DB
      val writeStartTime = System.currentTimeMillis()
      scala.concurrent.Future {
        requestData.request = checkNullOrEmpty(content)
        requestData.storeServiceActionAndStatusInCache()
        val id = RequestData.insert(requestData)
        Robot.talk(requestData)
      }.map {
        result =>
          Logger.debug("Request Data written to DB in " + (System.currentTimeMillis() - writeStartTime) + " ms")
      }
    } catch {
      case e: Throwable => Logger.error("Error writing to DB", e)
    }
  }

  private def processError(step: String, formatResponse: String, exception: Throwable) {
    Logger.error("Error on step " + step, exception)

    if (response == null) {
      response = new ClientResponse(null, -1)
    }

    val stackTraceWriter = new StringWriter
    exception.printStackTrace(new PrintWriter(stackTraceWriter))
    if (formatResponse == "xml") {
      response.body = faultXmlResponse("Server", exception.getMessage, stackTraceWriter.toString)
    } else if (formatResponse == "json") {
      response.body = faultJsonResponse("Server", exception.getMessage, stackTraceWriter.toString)
    }
    else {
      response.body = faultTextResponse("Server", exception.getMessage, stackTraceWriter.toString)
    }

    requestData.response = response.body
    requestData.status = Status.INTERNAL_SERVER_ERROR
  }

  private def faultJsonResponse(faultCode: String, faultString: String, faultMessage: String): String = {
    "{\"faultcode\":\"" + faultCode + "\", \"faultstring\":\"" + faultString + "\", \"detail\":\"" + faultMessage + "\"}"
  }

  private def faultXmlResponse(faultCode: String, faultString: String, faultMessage: String): String = {
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<SOAP-ENV:Envelope xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding\"  xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"> " +
      "<SOAP-ENV:Header/>" +
      "<SOAP-ENV:Body>" +
      "<SOAP-ENV:Fault>" +
      "<faultcode>SOAP-ENV:" + faultCode + "</faultcode>   " +
      "<faultstring>" + faultString + "</faultstring>   " +
      "<detail><reason>" + faultMessage + "</reason></detail>  " +
      "</SOAP-ENV:Fault>" +
      "</SOAP-ENV:Body>" +
      "</SOAP-ENV:Envelope>"
  }

  private def faultTextResponse(faultCode: String, faultString: String, faultMessage: String): String = {
    "FaultCode: " + faultCode + ", faultString: " + faultString + ", faultMessage: " + faultMessage
  }
}


class ClientResponse(wsResponse: Response = null, val responseTimeInMillis: Long) {

  var body: String = if (wsResponse != null) wsResponse.body else ""
  var contentType: String = if (wsResponse != null && wsResponse.getAHCResponse != null) wsResponse.getAHCResponse.getContentType else ""
  var bodyBytes = if (wsResponse != null && wsResponse.getAHCResponse != null) wsResponse.getAHCResponse.getResponseBodyAsBytes else null
  val status: Int = if (wsResponse != null) wsResponse.status else Status.INTERNAL_SERVER_ERROR

  private val headersNing: Map[String, Seq[String]] = if (wsResponse != null) ningHeadersToMap(wsResponse.getAHCResponse.getHeaders()) else Map()

  var headers: Map[String, String] = Map()

  // if more than one value for one header, take the last only
  headersNing.foreach(header => headers += header._1 -> header._2.last)

  private def ningHeadersToMap(headersNing: FluentCaseInsensitiveStringsMap) = {
    import scala.collection.JavaConverters._
    val res = mapAsScalaMapConverter(headersNing).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap
    TreeMap(res.toSeq: _*)(CaseInsensitiveOrdered)
  }
}



