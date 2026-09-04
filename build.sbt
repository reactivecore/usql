import xerial.sbt.Sonatype.GitHubHosting
import xerial.sbt.Sonatype.sonatypeCentralHost

// If there is a Tag starting with v, e.g. v0.3.0 use it as the build artefact version (e.g. 0.3.0)
val gitTag: Option[String] = sys.env.get("GITHUB_REF").flatMap { ref =>
  if (ref.startsWith("refs/tags/")) Some(ref.stripPrefix("refs/tags/"))
  else None
}

val versionTag = gitTag
  .filter(_.startsWith("v"))
  .map(_.stripPrefix("v"))

val snapshotVersion = "0.5-SNAPSHOT"
val artefactVersion = versionTag.getOrElse(snapshotVersion)

ThisBuild / scalacOptions ++= Seq("-feature")

def publishSettings = Seq(
  publishTo               := sonatypePublishToBundle.value,
  sonatypeCredentialHost  := sonatypeCentralHost,
  sonatypeBundleDirectory := (ThisBuild / baseDirectory).value / "target" / "sonatype-staging" / s"${version.value}",
  licenses                := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage                := Some(url("https://github.com/reactivecore/usql")),
  sonatypeProjectHosting  := Some(GitHubHosting("reactivecore", "usql", "contact@reactivecore.de")),
  developers              := List(
    Developer(
      id = "nob13",
      name = "Norbert Schultz",
      email = "norbert.schultz@reactivecore.de",
      url = url("https://www.reactivecore.de")
    )
  ),
  publish / test          := {},
  publishLocal / test     := {}
)

ThisBuild / version      := artefactVersion
ThisBuild / organization := "net.reactivecore"
ThisBuild / scalaVersion := "3.9.0"
ThisBuild / Test / fork  := true
ThisBuild / scalacOptions ++= Seq(
  "-new-syntax",
  "-rewrite",
  "-Wunused:all",
  "-Wconf:any:e",                                      // All Warnings are errors
  "-Wconf:src=src_managed/.*:silent",                  // No Warnings inside generated code
  "-Wconf:msg=unused private member&src=test/*:silent" // Do not care about unused stuff in Testcases
)

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

val scalaTestVersion = "3.2.20"

lazy val root = (project in file("."))
  .settings(
    name := "usql",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest"          % scalaTestVersion % Test,
      "org.scalatest" %% "scalatest-flatspec" % scalaTestVersion % Test,
      "com.h2database" % "h2"                 % "2.4.240"        % Test,
      "org.postgresql" % "postgresql"         % "42.7.11"        % Test
    ),
    publishSettings
  )

addCommandAlias("lint", "scalafmtAll;scalafixAll")

addCommandAlias("lintCheck", "scalafmtCheckAll; scalafixAll --check")
