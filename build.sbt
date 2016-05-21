
val Versions = new {
  val akka = "2.4.6"
}

lazy val commonSettings = Seq(
  organization := "de.aktey.akka.visualmailbox",
  version := "1.0-SNAPSHOT",

  scalaVersion := "2.11.8",

  scalacOptions ++= Seq("-deprecation", "-feature"),

  homepage := Some(url("https://github.com/ouven/akka-visualmailbox/wiki")),
  licenses := Seq(
    "Apache License Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    "The New BSD License" -> url("http://www.opensource.org/licenses/bsd-license.html")
  ),

  publishTo <<= version { v: String =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,

  pomIncludeRepository := { _ => false },
  pomExtra := <issueManagement>
    <system>github</system>
    <url>https://github.com/ouven/akka-visualmailbox/issues</url>
  </issueManagement>
    <developers>
      <developer>
        <name>Ruben Wagner</name>
        <url>https://github.com/ouven</url>
        <roles>
          <role>owner</role>
          <role>developer</role>
        </roles>
        <timezone>+1</timezone>
      </developer>
    </developers>
    <scm>
      <url>git@github.com:ouven/visualmailbox.git</url>
      <connection>scm:git:git@github.com:ouven/visualmailbox.git</connection>
      <developerConnection>scm:git:git@github.com:ouven/akka-visualmailbox.git</developerConnection>
    </scm>,

  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % Versions.akka
  )
)

lazy val root = project.in(file("."))
  .aggregate(collector, common, visualization)
  .settings(commonSettings: _*)
  .settings(Seq(
    publishArtifact := false
  ))

lazy val common = project
  .settings(commonSettings: _*)
  .settings(Seq(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.13.0" % "test"
    )
  ))

lazy val collector = project
  .dependsOn(common)
  .settings(commonSettings: _*)
  .settings(Seq())

lazy val visualization = project
  .dependsOn(common)
  .settings(commonSettings: _*)
  .settings(Seq())
