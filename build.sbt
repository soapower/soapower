import play.Project._

name         := "soapower"

version      := "1.1.2"

libraryDependencies ++= Seq(
    jdbc,
    anorm,
    cache,
    "mysql" % "mysql-connector-java" % "5.1.21")

playScalaSettings

requireJs += "soapower.js"

requireJsShim += "soapower.js"

