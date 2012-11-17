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

import play.api._

class Client {
    
  def send(remoteTarget: String, req: String, headers: Map[String, String]) = {

    val url = new URL(remoteTarget);
    val host = url.getHost
    val port = if (url.getPort < 0) 80 else url.getPort
    val path = url.getPath

    Logger.debug("RemoteTarget " + remoteTarget + " detail:" + host +":" + port + ""+ path)
    Logger.debug("Content " + req)

    val client: com.twitter.finagle.Service[HttpRequest, HttpResponse] = 
    ClientBuilder().codec(Http())
                   .hosts(new InetSocketAddress(host, port))
                   //.tls(host)
                   .hostConnectionLimit(1)
                   .build()

    val payload = req.getBytes("UTF-8")
    val request: HttpRequest = RequestBuilder().url(url)
        .addHeaders(headers)
        .buildPost(wrappedBuffer(payload))

    val f = client(request) 
    // Handle the response:
    f onSuccess { res =>
      Logger.info("got response" + res)
    } onFailure { exc =>
      Logger.info("failed :-(" + exc)
    }
    Logger.info("got response headers:" + f.get().getHeaders())
    Logger.info("got response content:" + f.get().getContent().toString())
    f.get()


    //val response = client.get() 
    //client.get()
    //Logger.debug("Response form server: " + response)
  }

}