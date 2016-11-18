import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

val Versions = new {
  val akka = "2.4.12"
  val `akka-http` = "10.0.0-RC2"
}

lazy val commonSettings = Seq(
  organization := "de.aktey.akka.visualmailbox",

  scalaVersion := "2.12.0",

  crossScalaVersions := Seq("2.11.8", "2.12.0"),

  scalacOptions ++= Seq("-deprecation", "-feature"),

  homepage := Some(url("https://github.com/ouven/akka-visualmailbox/wiki")),
  licenses := Seq(
    "Apache License Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    "The New BSD License" -> url("http://www.opensource.org/licenses/bsd-license.html")
  ),

  sources in EditSource ++= (baseDirectory.value / ".doctmpl" / "README.md").get,
  targetDirectory in EditSource := baseDirectory.value,
  variables in EditSource += "version" -> version.value,

  // relase with sbt-pgp plugin
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := ReleaseProcess.steps,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.trim.endsWith("SNAPSHOT"))
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
      <url>git@github.com:ouven/akka-visualmailbox.git</url>
      <connection>scm:git:git@github.com:ouven/akka-visualmailbox.git</connection>
      <developerConnection>scm:git:git@github.com:ouven/akka-visualmailbox.git</developerConnection>
    </scm>,

  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % Versions.akka
  )
)

lazy val `akka-visualmailbox` = project.in(file("."))
  .aggregate(collector, common, visualization)
  .settings(commonSettings: _*)
  .settings(Seq(
    publishArtifact := false
  ))

lazy val common = project
  .settings(commonSettings: _*)
  .settings(Seq(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
    )
  ))

lazy val collector = project
  .dependsOn(common)
  .settings(commonSettings: _*)
  .settings(Seq())

lazy val visualization = project
  .enablePlugins(JavaAppPackaging)
  .dependsOn(common)
  .settings(commonSettings: _*)
  .settings(Seq(
    resolvers += Resolver.bintrayRepo("hseeberger", "maven"),

    publishArtifact := false,

    libraryDependencies ++= Seq(
      "de.heikoseeberger" %% "akka-sse" % "2.0.0-M4",
      "com.typesafe.akka" %% "akka-http" % Versions.`akka-http`,
      "com.typesafe.akka" %% "akka-slf4j" % Versions.akka,
      "ch.qos.logback" % "logback-classic" % "1.1.7"
    ),

    dockerBaseImage := "java:jre-alpine",
    dockerExposedPorts := Seq(8080, 60009),
    packageName in Docker := "ouven/akka-visual-mailbox-visualization",
    dockerCommands := {
      val insertPoint = 2
      dockerCommands.value.take(insertPoint) ++ Seq(
        Cmd("USER", "root"),
        ExecCmd("RUN", "apk", "--update", "add", "bash")
      ) ++ dockerCommands.value.drop(insertPoint)
    },
    dockerUpdateLatest := !version.value.endsWith("SNAPSHOT")
  ))

lazy val `sample-project` = project
  .dependsOn(collector)
  .settings(commonSettings: _*)
  .settings(Seq(
    publishArtifact := false
  ))
