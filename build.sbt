import play.Project._

name         := "soapower"

version      := "2.0.0.Alpha1"

libraryDependencies ++= Seq(
    cache,
    "org.reactivemongo" %% "reactivemongo" % "0.10.0",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2")

playScalaSettings

mappings in Universal <++= baseDirectory map { dir => (dir / "soapowerctl.sh").*** --- dir x relativeTo(dir) }

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](
  name,
  version,
  scalaVersion,
  sbtVersion,
  BuildInfoKey.map(name) { case (k, v) => "project" + k.capitalize -> v.capitalize },
  BuildInfoKey.map(version) { case (k, v) => "versionDoc" -> (v.dropRight(1) + "x") } // version 2.0.0 -> 2.0.x
)

buildInfoPackage := "soapower.build.info"
