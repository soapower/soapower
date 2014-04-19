import play.Project._

name         := "soapower"

version      := "2.0.0"

libraryDependencies ++= Seq(
    cache,
    "org.reactivemongo" %% "reactivemongo" % "0.10.0",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2")

playScalaSettings

mappings in Universal <++= baseDirectory map { dir => (dir / "soapowerctl.sh").*** --- dir x relativeTo(dir) }
