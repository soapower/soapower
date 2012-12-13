import java.util.{ GregorianCalendar, Date }
import models.{ UtilDate, Environment, RequestData }
import play.api._

import play.api.libs.concurrent._

import akka.actor._
import scala.concurrent.duration._
import play.api.libs.concurrent._

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Starting Soapower...")

    Akka.system.scheduler.schedule(0 seconds, 30 minutes) {
      Environment.purgeXmlData()
    }

  }

  override def onStop(app: Application) {
    Logger.info("Soapower shutdown...")
  }

}
