import sbt.Keys.{name, _}
import sbt.{Credentials, Path, addArtifact}
import sbtassembly.AssemblyPlugin.autoImport.assemblyMergeStrategy
import sbtassembly.{MergeStrategy, PathList}

scalaVersion := "2.11.12"
val artifactory = "https://repos.apixio.com/artifactory"

lazy val commonSettings = Seq(
  organization := "apixio",
  parallelExecution in Test := false,
  publishMavenStyle := true,
  credentials ++= Seq(
  	Credentials(Path.userHome / ".ivy2" / "artifactory-build.credentials"),
  	Credentials(Path.userHome / ".ivy2" / "artifactory-release.credentials")
  ),
  resolvers ++= Seq(
    "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository",
    "Local Ivy Repository" at "file:///"+Path.userHome+"/.ivy2/local",
    "Apixio Release and Snapshots" at s"$artifactory/apixio-release-snapshot-jars/"
  ),
publishTo := {
  if (isSnapshot.value) {
    Some("Artifactory Realm" at artifactory + "/snapshots;build.timestamp=" + new java.util.Date().getTime)
  }
  else {
    Some("Artifactory Realm" at artifactory + "/releases")
  }
},
  (testOptions in Test) += Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/scalatest-report"),
  scalaVersion := "2.11.12", //2.12.5
  test in assembly := {}
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "apx-algosvc",
    scalacOptions in(Compile, doc) := Seq("-groups", "-implicits"),
    scalacOptions += "-deprecation",
    updateOptions := updateOptions.value.withCachedResolution(true),

    artifact in(Compile, assembly) := {
      val art = (artifact in(Compile, assembly)).value
      art.withClassifier(Some("assembly"))
    },
    excludeFilter in Compile := "spring.tooling",

    addArtifact(artifact in(Compile, assembly), assembly),
    mainClass in assembly := Some("com.apixio.algoservice.AlgorithmServiceApplication"),
    assemblyMergeStrategy in assembly := {
      case x if x.endsWith(".properties") => MergeStrategy.last
      case PathList("org", "apache", "commons", "logging", "impl", xs@_*) => MergeStrategy.first
      case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
      case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
      case PathList("org", "slf4j", "impl", xs@_*) => MergeStrategy.first
      case PathList("org", "slf4j", "log4j-over-slf4j", xs@_*) => MergeStrategy.discard
      case PathList("org", "joda", "time", xs@_*) => MergeStrategy.first
      case PathList("javax", "inject", xs@_*) => MergeStrategy.last
      case PathList("org", "aopalliance", ps@_*) => MergeStrategy.first
      case PathList("junit", xs@_ *) => MergeStrategy.discard
      case PathList("com", "sun", "research", "ws", "wadl", xs@_ *) => MergeStrategy.last
      case PathList("javax", "servlet", xs@_ *) => MergeStrategy.first
      case PathList("javax", "ws", "rs", xs@_ *) => MergeStrategy.last
      case PathList("com", "apixio", "ensemble", xs@_*) => MergeStrategy.first
      case PathList("com", "google", "inject", "multibindings", xs@_*) => MergeStrategy.first
      case PathList("com", "google", "inject", "multibindings", xs@_*) => MergeStrategy.first
      case PathList("config.yaml") => MergeStrategy.first
      case x if Assembly.isConfigFile(x) =>
        MergeStrategy.concat
      case PathList(ps@_*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
        MergeStrategy.rename
      case PathList("META-INF", xs@_*) =>
        (xs map {
          _.toLowerCase
        }) match {
          case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
            MergeStrategy.discard
          case ps@(x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
            MergeStrategy.discard
          case "plexus" :: xs =>
            MergeStrategy.discard
          case "services" :: xs =>
            MergeStrategy.filterDistinctLines
          case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
            MergeStrategy.filterDistinctLines
          case "spring.tooling" :: xs => MergeStrategy.first
          case _ => MergeStrategy.deduplicate
        }
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    updateOptions := updateOptions.value.withGigahorse(false)
  )
val jacksonVersion = "2.9.4"
