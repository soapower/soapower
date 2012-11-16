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

    def send(host: String, port: Int, path: String, req: String, headers: Map[String, String]) = {
    val clientService: com.twitter.finagle.Service[HttpRequest, HttpResponse] = 
      ClientBuilder().codec(Http())
                     .hosts(new InetSocketAddress(host, port))
                     //.tls(host)
                     .hostConnectionLimit(1)
                     .build()

    val payload = req.getBytes("UTF-8")
    val request: HttpRequest = RequestBuilder().url(new URL("http", host, port, path))
          .addHeaders(headers)
          .buildPost(wrappedBuffer(payload))

    Logger.debug("Server " + new URL("http", host, port, path).toString)
    Logger.debug("Content " + req)

    val client = clientService(request) 
    val response = client.get() 
    Logger.debug("Response form server: " + response)
  }

}