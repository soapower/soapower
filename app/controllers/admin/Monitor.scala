package controllers.admin

import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import java.util.Date
import ch.qos.logback.classic.spi.ILoggingEvent
import java.text.SimpleDateFormat
import models.Client
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.{Level, LoggerContext}
import scala.collection.JavaConversions._


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

  implicit val loggersFormat = Json.format[JsonLogger]

  def loggers = Action {
    val loggerContext: LoggerContext = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
    val loggersList = scala.collection.mutable.ListBuffer[JsonLogger]()
    loggerContext.getLoggerList.toList.foreach {
      logger =>
        loggersList.add(new JsonLogger(logger.getName, logger.getEffectiveLevel().toString()))
    }
    Ok(Json.toJson(loggersList)).as(JSON)
  }

  def changeLevel(loggerName: String, newLevel: String) = Action {
    val loggerContext: LoggerContext = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
    loggerContext.getLogger(loggerName).setLevel(Level.valueOf(newLevel))
    Ok("Done")
  }

}

case class JsonLogger(name: String, level: String)

object Streams {

  private val timeRefreshMillis = 1100

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

