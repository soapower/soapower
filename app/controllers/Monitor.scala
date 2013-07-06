package controllers

import play.api.mvc._
import play.api.libs._
import play.api.libs.json.{Json, JsValue}
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.stm._
import scala.concurrent.duration._
import java.io.File
import play.api.Logger
import java.util.Date
import ch.qos.logback.classic.spi.ILoggingEvent
import java.text.SimpleDateFormat
import models.Client

/*
* Code inspired from https://github.com/playframework/Play20/tree/master/samples/scala/comet-live-monitoring
*/
object Monitor extends Controller {

  private val logFile = play.api.Play.current.configuration.getString("soapower.log.file").get

  def downloadLogFile = Action {
    Ok.sendFile(new java.io.File(logFile))
  }

  def socket = WebSocket.using[String] {
    request =>

      val in = Iteratee.ignore[String]

      val out = Streams.getCPU >-
        Streams.getHeap >-
        Streams.getTotalMemory >-
        Streams.liveEnumerator >-
        Streams.getNbRequests

      (in, out)
  }

  def logfile = Action {
    Ok(Json.toJson(logFile)).as(JSON)
  }

  def gc = Action {
    Runtime.getRuntime().gc()
    Ok("Done")
  }
}

object Streams {

  private val timeRefreshMillis = 800

  private val timeRefreshMillisLong = 5000

  private val dateFormat = new SimpleDateFormat("HH:mm:ss.SSSS")

  val getHeap = Enumerator.generateM({
    Promise.timeout(
      Some((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + ":memory"),
      timeRefreshMillis, TimeUnit.MILLISECONDS)
  })

  val cpu = new models.CPU()

  val getCPU = Enumerator.generateM({
    Promise.timeout(Some((cpu.getCpuUsage() * 1000).round / 10.0 + ":cpu"), timeRefreshMillis, TimeUnit.MILLISECONDS)
  })

  val getTotalMemory = Enumerator.generateM({
    Promise.timeout(
      Some(Runtime.getRuntime().totalMemory() / (1024 * 1024) + ":totalMemory")
      , timeRefreshMillis, TimeUnit.MILLISECONDS)
  })

  val (liveEnumerator, channelLogs) = Concurrent.broadcast[String]

  def pushLog(event: ILoggingEvent) {
    channelLogs.push(views.html.monitor.log.render(dateFormat.format(new Date(event.getTimeStamp)), event).toString)
  }

  val getNbRequests = Enumerator.generateM({
    Promise.timeout(
      Some(Client.getNbRequest + ":nbReq"), timeRefreshMillisLong, TimeUnit.MILLISECONDS)
  })
}

