package controllers

import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.stm._
import scala.concurrent.duration._
import java.io.File
import play.api.Logger

/*
* Code from https://github.com/playframework/Play20/tree/master/samples/scala/comet-live-monitoring
*/
object Monitor extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.monitor.index())
  }

  def socket = WebSocket.using[String] { request =>

    val in = Iteratee.ignore[String]

    val out = Streams.getCPU >-
      Streams.getHeap >-
      Streams.getTotalMemory

    (in, out)

  }

  def gc = Action {
    Runtime.getRuntime().gc()
    Ok("Done")
  }
}

object Streams {

  val timeRefreshMillis = 500

  val timeRefreshMillisLong = 2000

  val getRequestsPerSecond = Enumerator.generateM({
    Promise.timeout({
      Some(SpeedOMeter.getSpeed + ":rps")
    },
      timeRefreshMillis, TimeUnit.MILLISECONDS)
  })

  val getHeap = Enumerator.generateM({
    Promise.timeout(
      Some((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + ":memory"),
      timeRefreshMillis, TimeUnit.MILLISECONDS)
  })

  val cpu = new models.CPU()

  val getCPU = Enumerator.generateM({
    Promise.timeout(Some((cpu.getCpuUsage() * 1000).round / 10.0 + ":cpu"), timeRefreshMillis, TimeUnit.MILLISECONDS)
  })

  val getTotalMemory =Enumerator.generateM({
    Promise.timeout(
      Some(Runtime.getRuntime().totalMemory() / (1024 * 1024) + ":totalMemory")
      , timeRefreshMillis, TimeUnit.MILLISECONDS)
  })


}

object SpeedOMeter {

  val unit = 100

  private val counter = Ref((0, (0, java.lang.System.currentTimeMillis())))

  def countRequest() = {
    val current = java.lang.System.currentTimeMillis()
    counter.single.transform {
      case (precedent, (count, millis)) if current > millis + unit => (0, (1, current))
      case (precedent, (count, millis)) if current > millis + (unit / 2) => (count, (1, current))
      case (precedent, (count, millis)) => (precedent, (count + 1, millis))
    }
  }

  def getSpeed = {
    val current = java.lang.System.currentTimeMillis()
    val (precedent, (count, millis)) = counter.single()
    val since = current - millis
    if (since <= unit) ((count + precedent) * 1000) / (since + unit / 2)
    else 0.toLong
  }

}
