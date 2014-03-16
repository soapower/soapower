import play.Project._

name         := "soapower"

version      := "2.0.0"

libraryDependencies ++= Seq(
    jdbc,
    anorm,
    cache,
    "org.reactivemongo" %% "reactivemongo" % "0.10.0",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",
    "mysql" % "mysql-connector-java" % "5.1.21")

playScalaSettings

mappings in Universal <++= baseDirectory map { dir => (dir / "soapowerctl.sh").*** --- dir x relativeTo(dir) }
