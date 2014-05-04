import play.PlayImport.PlayKeys
import play.PlayScala
import play.Project._

name         := "soapower"

version      := "2.0.0"

libraryDependencies ++= Seq(
    cache,
    PlayKeys.ws,
    "org.reactivemongo" %% "reactivemongo" % "0.10.0",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2")


lazy val root = (project in file(".")).addPlugins(PlayScala)


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
