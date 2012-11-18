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

import play.Logger
import play.api._

class Client(remoteTarget: String, req: String, headersOut: Map[String, String]) {
    
    val url = new URL(remoteTarget);
    val host = url.getHost
    val port = if (url.getPort < 0) 80 else url.getPort
    val path = url.getPath

    Logger.debug("RemoteTarget " + remoteTarget + " detail:" + host +":" + port + ""+ path)
    Logger.debug("Content " + req)

    val service : com.twitter.finagle.Service[HttpRequest, HttpResponse] = 
    ClientBuilder().codec(Http())
                   .hosts(new InetSocketAddress(host, port))
                   //.tls(host)
                   //.hostConnectionLimit(Integer.MAX_VALUE)
                   .tcpConnectTimeout(Duration(10, TimeUnit.SECONDS))
                   .hostConnectionLimit(1)
                   .build()

    val payload = req.getBytes("UTF-8")
    val request: HttpRequest = RequestBuilder().url(url)
        .addHeaders(headersOut)
        .buildPost(wrappedBuffer(payload))


    val f = service(request) 
    // Handle the response:
    f onSuccess { res =>
      Logger.debug("got response" + res)
      Logger.debug("got response headers:" + f.get().getHeaders())
    } onFailure { exc =>
      Logger.error("failed :-(" + exc)
    }

    var headers:Map[String, String] = Map()
    var lst = f.get().getHeaders().toList
    lst.foreach{e => 
      headers += (e.getKey -> e.getValue)
    }

    var response = ""
    for(r <- f) {
      response = r.getContent.toString("UTF-8")
    }
    Logger.debug("got response content:" + response)

}