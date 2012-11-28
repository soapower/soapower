package controllers

import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Promise
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.stm._

/*
* Code from https://github.com/playframework/Play20/tree/master/samples/scala/comet-live-monitoring
*/
object Monitor extends Controller {

  def load = Action {
    SpeedOMeter.countRequest()
    Ok("from your shell ab -k -c 100 -n 1000000 http://localhost:9000/load")
  }

  def index = Action {
    Ok(views.html.monitor.index(Runtime.getRuntime().totalMemory() / (1024 * 1024)))
  }

  def monitoring = Action {
    Ok.stream(
      Streams.getRequestsPerSecond >-
        Streams.getCPU >-
        Streams.getHeap &>
        Comet(callback = "parent.message"))
  }

  def gc = Action {
    Runtime.getRuntime().gc()
    Ok("Done")
  }
}

object Streams {

  val timeRefreshMillis = 500

  val getRequestsPerSecond = Enumerator.fromCallback { () =>
    Promise.timeout({
      val currentMillis = java.lang.System.currentTimeMillis()
      Some(SpeedOMeter.getSpeed + ":rps")
    },
      timeRefreshMillis, TimeUnit.MILLISECONDS)
  }

  val getHeap = Enumerator.fromCallback { () =>
    Promise.timeout(
      Some((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024) + ":memory"),
      timeRefreshMillis, TimeUnit.MILLISECONDS)
  }

  val cpu = new models.CPU()

  val getCPU = Enumerator.fromCallback { () =>
    Promise.timeout(
      Some((cpu.getCpuUsage() * 1000).round / 10.0 + ":cpu:" + Runtime.getRuntime().totalMemory() / (1024 * 1024)),
      timeRefreshMillis, TimeUnit.MILLISECONDS)
  }

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
    else 0
  }

}
