import play.Project._

name         := "soapower"

version      := "1.3.1"

libraryDependencies ++= Seq(
    jdbc,
    anorm,
    cache,
    "mysql" % "mysql-connector-java" % "5.1.21")

playScalaSettings


mappings in Universal <++= baseDirectory map { dir => (dir / "soapowerctl.sh").*** --- dir x relativeTo(dir) }
