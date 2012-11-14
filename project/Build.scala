import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "soapower"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "com.twitter" % "finagle-core" % "5.3.22",
      "com.twitter" % "finagle-http" % "5.3.22"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
