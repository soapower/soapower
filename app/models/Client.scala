package models

import java.net.InetSocketAddress
import scala.xml.{Elem, XML}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.finagle.builder.ClientBuilder;
import com.twitter.finagle.http.{Http, RequestBuilder};
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer
import java.net.URL
import com.twitter.util.{Duration}
import java.util.concurrent.TimeUnit
import java.util.HashMap
import scala.collection.JavaConversions._
import com.twitter.finagle._

import play.Logger
import play.api._

class Client(remoteTarget: String, timeoutms: Long, req: String, headersOut: Map[String, String]) {
    
    val url = new URL(remoteTarget);
    val host = url.getHost
    val port = if (url.getPort < 0) 80 else url.getPort
    val path = url.getPath

    var headers:Map[String, String] = Map()
    var response = ""

    Logger.debug("RemoteTarget " + remoteTarget + " detail:" + host +":" + port + ""+ path + " content :" + req)

    val service : com.twitter.finagle.Service[HttpRequest, HttpResponse]  = 
    ClientBuilder().codec(Http())
                   .hosts(new InetSocketAddress(host, port))
                   //.tls(host)
                   //.hostConnectionLimit(Integer.MAX_VALUE)
                   .tcpConnectTimeout(Duration(1000, TimeUnit.MILLISECONDS))
                   .timeout(Duration(timeoutms, TimeUnit.MILLISECONDS))
                   .hostConnectionLimit(1)
                   .build()

    val request: HttpRequest = RequestBuilder().url(url)
        .addHeaders(headersOut)
        .buildPost(wrappedBuffer(req.getBytes("UTF-8")))

    val f = service(request) 

    // Handle the response:
    f onSuccess { res =>
      Logger.debug("got response" + res)
    } onFailure { e =>
      e match {
        case e:GlobalRequestTimeoutException => Logger.error("Timeout exception : " + e.getMessage)
        case _ => Logger.error("Exception !")
      }
    }
    
    try {
      var lst = f.get().getHeaders().toList
      lst.foreach{e => 
          headers += (e.getKey -> e.getValue)
      }
    } catch {
      // already done
      case _ => Logger.debug("Exception caught. See onFailure!")
    }

    for(r <- f) {
      response = r.getContent.toString("UTF-8")
    }
    Logger.debug("got response content:" + response)
  
}