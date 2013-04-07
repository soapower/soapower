import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "soapower"
    val appVersion      = "1.0"

    val appDependencies = Seq(
        jdbc,
        anorm,
        "mysql" % "mysql-connector-java" % "5.1.21"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(

    )

}
