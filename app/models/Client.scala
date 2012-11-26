package models

import com.ning.http.client.Realm.AuthScheme
import com.ning.http.client.FluentCaseInsensitiveStringsMap

import java.net.InetSocketAddress
import java.net.URL

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.xml._
import akka.util.Timeout

import play.api._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.core.utils.CaseInsensitiveOrdered
import play.Logger

import collection.immutable.TreeMap

class Client(service: Service, content: String, headersOut: Map[String, String]) {

  var response: ClientResponse = null

  private val url = new URL(service.remoteTarget);
  private val host = url.getHost
  private val port = if (url.getPort < 0) 80 else url.getPort
  private var future: Future[Response] = null
  private var requestTimeInMillis: Long = -1

  def sendRequest() {
    if (Logger.isDebugEnabled) {
      //Logger.debug("RemoteTarget " + service.remoteTarget + " content=" + Utility.trim(XML.loadString(content)))
      Logger.debug("RemoteTarget " + service.remoteTarget)
    }

    requestTimeInMillis = System.currentTimeMillis
    var wsRequestHolder = WS.url(service.remoteTarget).withTimeout(service.timeoutms.toInt)
    val headers = for ((key, value) <- headersOut) wsRequestHolder.withHeaders((key, value))

    if (service.user.isDefined && service.password.isDefined) {
      wsRequestHolder.withAuth(service.user.get, service.password.get, AuthScheme.BASIC)
    }

    future = wsRequestHolder.post(content)
  }

  def waitForResponse() {
    try {
      val wsResponse: Response = Await.result(future, service.timeoutms.millis * 1000000)
      response = new ClientResponse(wsResponse, (System.currentTimeMillis - requestTimeInMillis))

      if (Logger.isDebugEnabled) {
        //Logger.debug("Reponse in " + (responseTimeInMillis - requestTimeInMillis) + " ms, content=" + Utility.trim(wsresponse.xml))
        Logger.debug("Reponse in " + response.responseTimeInMillis + " ms")
      }
    } catch {
      case e: Throwable => Logger.error("Error: " + e.getMessage)
    }
  }

  class ClientResponse(wsResponse: Response, val responseTimeInMillis: Long) {
    val body: String = wsResponse.body
    val status: Int = wsResponse.status;

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

}
