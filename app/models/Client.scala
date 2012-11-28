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
import play.core.utils.CaseInsensitiveOrdered
import play.Logger
import collection.immutable.TreeMap
import play.api.mvc.Request
import play.api.mvc.AnyContent

class Client(service: Service, request: Request[AnyContent]) {

  val sender = request.remoteAddress
  val content: String = request.body.asXml.get.toString()
  val headersOut: Map[String, String] = request.headers.toSimpleMap
  val requestData = new RequestData(sender, request.headers("SOAPACTION"), service.environmentId, service.localTarget, service.remoteTarget, content)
  var response: ClientResponse = null

  private var future: Future[Response] = null
  private var requestTimeInMillis: Long = -1

  def sendRequest() {
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

    // handle authentication
    if (service.user.isDefined && service.password.isDefined) {
      wsRequestHolder = wsRequestHolder.withAuth(service.user.get, service.password.get, AuthScheme.BASIC)
    }

    // perform request
    future = wsRequestHolder.post(content)
  }

  def waitForResponse() {
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
      case e: Throwable => Logger.error("Error: " + e.getMessage)
    }
  }

  private def prepareRequestData() {
    requestData.timeInMillis = response.responseTimeInMillis
    requestData.response = response.body
    requestData.status = response.status

    var soapAction = request.headers("SOAPACTION")
    
    // drop apostrophes if present
    if(soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
      soapAction = soapAction.drop(1).dropRight(1)
    }
    
    requestData.setSoapActionAndPutInCache(soapAction)
  }

}

class ClientResponse(wsResponse: Response, val responseTimeInMillis: Long) {

  val body: String = wsResponse.body
  val status: Int = wsResponse.status

  private val headersNing: Map[String, Seq[String]] = ningHeadersToMap(wsResponse.getAHCResponse.getHeaders())

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
