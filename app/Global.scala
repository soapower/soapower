import controllers.admin.ServiceActions
import models.{Robot, LiveRoom, Environment}
import play.api._

import scala.concurrent.duration._
import play.api.libs.concurrent._

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Starting Soapower " + soapower.build.info.BuildInfo.version + "...")

    LiveRoom.init

    // initialDelay: Duration : 10 minutes
    // frequency: Duration : 5 hours
    Akka.system.scheduler.schedule(30 minutes, 5 hours) {
      ServiceActions.regenerate()
      Environment.compileStats()
      Environment.purgeContentData()
      Environment.purgeAllData()
    }
  }

  override def onStop(app: Application) {
    Robot.talkMsg("Soapower is restarting... Please refresh your page", "error")
    Akka.system.awaitTermination(3 seconds) // time to let Robot talk
    Logger.info("Soapower shutdown...")
  }

}
