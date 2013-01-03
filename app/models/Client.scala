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

object Client {
  val queue = new scala.collection.mutable.Queue[RequestData]
  val maxQueueSize = 100

  def processQueue(requestData : RequestData) {
    val writeStartTime = System.currentTimeMillis()
    Akka.future {
      Client.queue += requestData
      if (Client.queue.size > maxQueueSize) {
        val req = Client.queue.dequeue
        Logger.debug("Request: " + req.toString)
      }
    }.map {
      result =>
        Logger.debug("Request Data store to queue in " + (System.currentTimeMillis() - writeStartTime) + " ms")
    }
  }
}

class Client(service: Service, sender: String, content: String, headers: Map[String, String]) {

  val requestData = new RequestData(sender, extractSoapAction(headers), service.environmentId, service.id.get)
  var response: ClientResponse = null

  private var futureResponse: Future[Response] = null
  private var requestTimeInMillis: Long = -1

  def sendRequestAndWaitForResponse() {
    if (Logger.isDebugEnabled) {
      Logger.debug("RemoteTarget " + service.remoteTarget)
    }

    requestTimeInMillis = System.currentTimeMillis

    // prepare request
    var wsRequestHolder = WS.url(service.remoteTarget).withTimeout(service.timeoutms.toInt)
    wsRequestHolder = wsRequestHolder.withHeaders((HeaderNames.X_FORWARDED_FOR -> sender))

    // add headers
    def filteredHeaders = headers.filterNot { _._1 == HeaderNames.TRANSFER_ENCODING }
    wsRequestHolder = wsRequestHolder.withHeaders(filteredHeaders.toArray : _*)
    wsRequestHolder = wsRequestHolder.withHeaders((HeaderNames.CONTENT_LENGTH -> content.getBytes.size.toString))

    try {
      // perform request
      futureResponse = wsRequestHolder.post(content)

      // wait for the response
      waitForResponse(headers, content)

    } catch {
      case e: Throwable => processError("post", e)
    }

    // save the request and response data to DB
    saveData(content)
  }

  private def waitForResponse(headers: Map[String, String], content: String) {
    try {
      val wsResponse: Response = Await.result(futureResponse, service.timeoutms.millis * 1000000)
      response = new ClientResponse(wsResponse, (System.currentTimeMillis - requestTimeInMillis))
      requestData.timeInMillis = response.responseTimeInMillis
      requestData.status = response.status
      Client.processQueue(requestData)
      requestData.requestHeaders = headers
      requestData.response = response.body
      requestData.responseHeaders = response.headers

      if (Logger.isDebugEnabled) {
        Logger.debug("Reponse in " + response.responseTimeInMillis + " ms")
      }
    } catch {
      case e: Throwable => processError("waitForResponse", e)
    }
  }

  private def saveData(content: String) {
    try {
      // asynchronously writes data to the DB
      val writeStartTime = System.currentTimeMillis()
      Akka.future {
        requestData.request = content
        requestData.storeSoapActionAndStatusInCache()
        val id = RequestData.insert(requestData)
        requestData.id = anorm.Id(id)
        Robot.talk(requestData)
      }.map {
        result =>
          Logger.debug("Request Data written to DB in " + (System.currentTimeMillis() - writeStartTime) + " ms")
      }
    } catch {
      case e: Throwable => Logger.error("Error writing to DB", e)
    }
  }



  private def extractSoapAction(headers: Map[String, String]): String = {
    var soapAction = headers.get("SOAPAction").get

    // drop apostrophes if present
    if (soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
      soapAction = soapAction.drop(1).dropRight(1)
    }

    soapAction
  }

  private def processError(step: String, exception: Throwable) {
    Logger.error("Error on step " + step, exception)

    if (response == null) {
      response = new ClientResponse(null, -1)
    }

    val stackTraceWriter = new StringWriter
    exception.printStackTrace(new PrintWriter(stackTraceWriter))
    response.body = faultResponse("Server", exception.getMessage, stackTraceWriter.toString)

    requestData.response = response.body
    requestData.status = Status.INTERNAL_SERVER_ERROR
  }

  private def faultResponse(faultCode: String, faultString: String, faultMessage: String): String = {
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
}

class ClientResponse(wsResponse: Response = null, val responseTimeInMillis: Long) {

  var body: String = if (wsResponse != null) wsResponse.body else ""
  val status: Int = if (wsResponse != null) wsResponse.status else Status.INTERNAL_SERVER_ERROR

  private val headersNing: Map[String, Seq[String]] = if (wsResponse != null) ningHeadersToMap(wsResponse.getAHCResponse.getHeaders()) else Map()

  var headers: Map[String, String] = Map()
  headersNing.foreach(header =>
    if (header._1 != "Transfer-Encoding")
      headers += header._1 -> header._2.last // if more than one value for one header, take the last only
  )

  private def ningHeadersToMap(headersNing: FluentCaseInsensitiveStringsMap) = {
    import scala.collection.JavaConverters._
    val res = mapAsScalaMapConverter(headersNing).asScala.map(e => e._1 -> e._2.asScala.toSeq).toMap
    TreeMap(res.toSeq: _*)(CaseInsensitiveOrdered)
  }

}


