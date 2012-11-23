package models

import java.net.InetSocketAddress
import java.net.URL

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
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
    var response:String  = ""

    init()

    private def init () {
      Logger.debug("RemoteTarget " + service.remoteTarget + " content :" + content)

      //TODO add headers
      val future = WS.url(service.remoteTarget)
                      .withHeaders(("Content-Type", "text/xml;charset=utf-8"))
                      .post(content)
      
      try {
        val result = Await.result(future, service.timeoutms milliseconds)
        response = result.body
        Logger.debug("reponse:" + response)
      } catch {
        case e:Throwable => Logger.error("Timeout: " + e.getMessage)
      }

      Logger.debug("client ended")
    }
}