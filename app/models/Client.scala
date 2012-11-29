package models

import com.ning.http.client.Realm.AuthScheme
import com.ning.http.client.FluentCaseInsensitiveStringsMap
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.xml._
import akka.util.Timeout
import play.api._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent._
import play.api.http._
import play.core.utils.CaseInsensitiveOrdered
import play.Logger
import collection.immutable.TreeMap
import play.api.mvc.Request
import play.api.mvc.AnyContent
import java.io.StringWriter
import java.io.PrintWriter

class Client(service: Service, request: Request[scala.xml.NodeSeq]) {

  val sender = request.remoteAddress
  val content: String = request.body.toString()
  val headersOut: Map[String, String] = request.headers.toSimpleMap
  val requestData = new RequestData(sender, request.headers("SOAPACTION"), service.environmentId, service.localTarget, service.remoteTarget, content)
  var response: ClientResponse = null

  private var future: Future[Response] = null
  private var requestTimeInMillis: Long = -1

  def sendRequestAndWaitForResponse() {
    if (Logger.isDebugEnabled) {
      //Logger.debug("RemoteTarget " + service.remoteTarget + " content=" + Utility.trim(XML.loadString(content)))
      Logger.debug("RemoteTarget " + service.remoteTarget)
    }

    requestTimeInMillis = System.currentTimeMillis

    // prepare request
    var wsRequestHolder = WS.url(service.remoteTarget).withTimeout(service.timeoutms.toInt)
    wsRequestHolder = wsRequestHolder.withHeaders(("X-Forwarded-For", sender))

    // add headers
    for ((key, value) <- headersOut) {
      // FIXME : accept gzip body
      if (key != "Accept-Encoding")
        wsRequestHolder = wsRequestHolder.withHeaders((key, value))
    }

    // perform request
    try {
      future = wsRequestHolder.post(content)

      // wait for the response
      waitForResponse()

    } catch {
      case e: Throwable => case e: Throwable => processError("post", e)
    }
  }

  private def waitForResponse() {
    try {
      val wsResponse: Response = Await.result(future, service.timeoutms.millis * 1000000)

      response = new ClientResponse(wsResponse, (System.currentTimeMillis - requestTimeInMillis))

      // asynchronously writes data to the DB
      val writeStartTime = System.currentTimeMillis()
      import play.api.Play.current
      Akka.future {
        prepareRequestData()
        RequestData.insert(requestData)
      }.map {
        result =>
          Logger.debug("Request Data written to DB in " + (System.currentTimeMillis() - writeStartTime) + " ms")
      }

      if (Logger.isDebugEnabled) {
        //Logger.debug("Reponse in " + (responseTimeInMillis - requestTimeInMillis) + " ms, content=" + Utility.trim(wsresponse.xml))
        Logger.debug("Reponse in " + response.responseTimeInMillis + " ms")
      }
    } catch {
      case e: Throwable => processError("waitForResponse", e)
    }
  }

  private def prepareRequestData() {
    requestData.timeInMillis = response.responseTimeInMillis
    requestData.response = response.body
    requestData.status = response.status

    var soapAction = request.headers("SOAPACTION")

    // drop apostrophes if present
    if (soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
      soapAction = soapAction.drop(1).dropRight(1)
    }

    requestData.setSoapActionAndPutInCache(soapAction)
  }

  private def processError(step: String, exception: Throwable) {
    Logger.error("Error on step " + step, exception)

    if (response == null)
      response = new ClientResponse(null, -1)

    val writer = new StringWriter
    exception.printStackTrace(new PrintWriter(writer))
    response.body = faultResponse("Server", exception.getMessage, writer.toString)
    requestData.response = response.body
    requestData.status = Status.INTERNAL_SERVER_ERROR
    RequestData.insert(requestData)
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


