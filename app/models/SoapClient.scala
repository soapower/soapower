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


class SoapClient {

    def wrap(xml: Elem): String = {
    val wrapper = <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
                <SOAP-ENV:Body>
                  {xml}
                </SOAP-ENV:Body>
              </SOAP-ENV:Envelope>
    wrapper.toString
  }

  
    def sendWithFinagle(host: String, path: String, req: Elem) = {
    val clientService: com.twitter.finagle.Service[HttpRequest, HttpResponse] =
ClientBuilder()
      .codec(Http())
      .hosts(new InetSocketAddress(host, 443))
      .tls(host)
      .hostConnectionLimit(1)
      .build()
    val payload = wrap(req).getBytes("UTF-8")
    val request: HttpRequest = RequestBuilder().url(new URL("https", host, 443, path))
                                                .setHeader("Content-Type", "text/xml")
                                                .setHeader("Content-Length", payload.length.toString)
                                                .buildPost(wrappedBuffer(payload))

    val client = clientService(request)

    for(response <- client) {
      println(response)
      println(response.getContent.toString("UTF-8"))
    }
    // val response = client.get()
  }
}