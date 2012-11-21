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
    var response = ""
    var data: Data = new Data(service.localTarget, service.remoteTarget, content);
    
    init()

    private def init () {
      Logger.debug("RemoteTarget " + service.remoteTarget + " content :" + content)

      implicit val timeout = Timeout(5 seconds) // needed for `?` below

      //TODO add headers
      val future = WS.url(service.remoteTarget).post(content);
      
      try {
        val result = Await.result(future, 5 seconds)
        data.response = result.body
        Logger.debug("reponse:" + data.response)
      } catch {
        case e:Throwable => Logger.error("Timeout: " + e.getMessage)
        data.state = "timeout"
      }

      Logger.debug("client ended")
    }
}