import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.info("Starting Soapower...")
  }

  override def onStop(app: Application) {
    Logger.info("Soapower shutdown...")
  }

}
