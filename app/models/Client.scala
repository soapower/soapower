package models

import com.ning.http.client.Realm.AuthScheme

import java.net.InetSocketAddress
import java.net.URL

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.xml._
import akka.util.Timeout

import play.api.libs.concurrent.Execution.Implicits.defaultContext 

import play.Logger
import play.api._
import play.api.libs.ws._


class Client(service: Service, content: String, headersOut: Map[String, String]) {
    
    val url = new URL(service.remoteTarget);
    val host = url.getHost
    val port = if (url.getPort < 0) 80 else url.getPort
    
    var headers:Map[String, String] = Map()
    var future:Future[Response] = null
    var response:String  = ""

    var requestTimeInMillis:Long = -1
    var responseTimeInMillis:Long = -1

    def sendRequest () {
      if (Logger.isDebugEnabled) {
        //Logger.debug("RemoteTarget " + service.remoteTarget + " content=" + Utility.trim(XML.loadString(content)))
        Logger.debug("RemoteTarget " + service.remoteTarget)
      }

      //TODO add headers
      requestTimeInMillis = System.currentTimeMillis
      future = WS.url(service.remoteTarget)
                      .withHeaders(("Content-Type", "text/xml;charset=utf-8"))
                      .withAuth("bios", "bios", AuthScheme.BASIC)
                      .withTimeout(service.timeoutms.toInt)
                      .post(content)
    }

    def waitForResponse () {
      try {
        val result = Await.result(future, service.timeoutms.millis * 1000000)
        responseTimeInMillis = System.currentTimeMillis
        response = result.body
        
        if (Logger.isDebugEnabled) {
          //Logger.debug("Reponse in " + (responseTimeInMillis - requestTimeInMillis) + " ms, content=" + Utility.trim(result.xml))
          Logger.debug("Reponse in " + (responseTimeInMillis - requestTimeInMillis) + " ms")
        }
      } catch {
        case e:Throwable => Logger.error("Error: " + e.getMessage)
      }
    }

}