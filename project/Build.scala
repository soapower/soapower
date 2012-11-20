import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "soapower"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
        "com.twitter" % "finagle-core" % "5.3.22",
        "com.twitter" % "finagle-http" % "5.3.22",
        jdbc,
        anorm
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here      
    )

}
