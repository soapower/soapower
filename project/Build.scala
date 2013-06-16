import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "soapower"
    val appVersion      = "1.1.0"

    val appDependencies = Seq(
        jdbc,
        anorm,
        "mysql" % "mysql-connector-java" % "5.1.21",
        "org.webjars" % "angularjs" % "1.1.5",
        "org.webjars" % "requirejs" % "2.1.1",
        "org.webjars" % "webjars-play" % "2.1.0-1")

    val main = play.Project(appName, appVersion, appDependencies).settings(

    )

}
