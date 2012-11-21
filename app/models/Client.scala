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
import java.nio.charset.Charset
import play.api.cache.Cache
import play.api.Play.current

import play.Logger
import play.api._

class Client(service: Service, content: String, headersOut: Map[String, String]) {
    
    val url = new URL(service.remoteTarget);
    val host = url.getHost
    val port = if (url.getPort < 0) 80 else url.getPort
    val path = url.getPath
    private val charset = Charset.forName("UTF-8");

    var headers:Map[String, String] = Map()
    var response = ""
    var data: Data = new Data(service.localTarget, service.remoteTarget, content);
    
    init()

    private def init () {
      Logger.debug("RemoteTarget " + service.remoteTarget + " detail:" + host +":" + port + ""+ path + " content :" + content)

      val serviceFinagle : com.twitter.finagle.Service[HttpRequest, HttpResponse]  = 
      ClientBuilder().codec(Http())
                     .hosts(new InetSocketAddress(host, port))
                     .tcpConnectTimeout(Duration(1000, TimeUnit.MILLISECONDS))
                     .timeout(Duration(service.timeoutms, TimeUnit.MILLISECONDS))
                     .hostConnectionLimit(1)
                     .build()

      val request: HttpRequest = RequestBuilder().url(url)
          .addHeaders(headersOut)
          .buildPost(wrappedBuffer(content.getBytes("UTF-8")))


      storeData
      val f = serviceFinagle(request) 

      // Handle the response:
      f onSuccess { res =>
        storeResponse(res)
      } onFailure { e =>
        storeFailure(e)
      }
      
      try {
        var lst = f.get().getHeaders().toList
        lst.foreach{e => 
            headers += (e.getKey -> e.getValue)
        }
      } catch {
        // already done
        case _ : Throwable => Logger.debug("Exception caught. See onFailure!")
      }

      for(r <- f) response = r.getContent.toString(charset)
      Logger.debug("got response content:" + response)
    }

    private def storeResponse(response : HttpResponse) {
      //TODO
      Logger.debug("got response" + response)

    }

    private def storeData() {
      Cache.set("item.key", data)
      //data = new Data(anorm.NotAssigned, service.localTarget, service.remoteTarget, content, "", "", "");
      //TODO
      // store timeoutms, request body, headers, use models.Data
    }

    private def storeFailure(e: Throwable) {
      //TODO, use models.Data
      e match {
          case e:GlobalRequestTimeoutException => Logger.error("Timeout exception : " + e.getMessage)
          case _ => Logger.error("Exception !")
      }
    }
}