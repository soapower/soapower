import play.Project._

name         := "soapower"

version      := "2.0.0"

libraryDependencies ++= Seq(
    jdbc,
    anorm,
    cache,
    "mysql" % "mysql-connector-java" % "5.1.21")

playScalaSettings


mappings in Universal <++= baseDirectory map { dir => (dir / "soapowerctl.sh").*** --- dir x relativeTo(dir) }
