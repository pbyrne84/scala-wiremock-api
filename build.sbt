lazy val baseName = "scala-wiremock-api"

lazy val scala213 = "2.13.10"
lazy val scala3 = "3.2.2"

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
publishMavenStyle := true

ThisBuild / dynverSonatypeSnapshots := true

inThisBuild(
  List(
    organization := "uk.org.devthings",
    homepage := Some(url("https://github.com/sbt/sbt-ci-release")),
    // Alternatively License.Apache2 see https://github.com/sbt/librarymanagement/blob/develop/core/src/main/scala/sbt/librarymanagement/License.scala
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "pbyrne84",
        "Patrick Byrne",
        "pbyrne84@gmail.com",
        url("https://devthings.org.uk/")
      )
    )
  )
)

scalaVersion := scala213

name := "scala-wiremock-api"
organization := "uk.org.devthings"
organizationHomepage := Some(url("https://scala.devthings.org.uk"))
description := "Scala adaptors for wiremock to make things a bit nicer"

crossScalaVersions := List(scala213, scala3)

val circeVersion = "0.14.5"
val sttpVersion: String = "3.8.11"

Test / parallelExecution := false

/**
  * <dependency> <groupId>uk.org.devthings</groupId> <artifactId>scala-wiremock-api_2.13</artifactId>
  * <version>0.1.0</version> </dependency>
  */

libraryDependencies ++= List(
  "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0" % Provided,
  "ch.qos.logback" % "logback-classic" % "1.4.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "io.circe" %% "circe-core" % circeVersion % Test,
  "io.circe" %% "circe-generic" % circeVersion % Test,
  "io.circe" %% "circe-parser" % circeVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test,
  "com.softwaremill.sttp.client3" %% "core" % sttpVersion % Test
)

scalacOptions ++= Seq(
  "-encoding",
  "utf8",
  "-feature",
  "-language:implicitConversions",
  "-language:existentials",
  "-unchecked"
) ++
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => Seq("-Ytasty-reader") // flags only needed in Scala 2
    case Some((3, _)) => Seq("-no-indent") // flags only needed in Scala 3
    case _ => Seq.empty
  })

//not to be used in ci, intellij has got a bit bumpy in the format on save on optimize imports across the project
val formatAndTest =
  taskKey[Unit]("format all code then run tests, do not use on CI as any changes will not be committed")

formatAndTest := {
  (Test / test)
    .dependsOn(Compile / scalafmtAll)
    .dependsOn(Test / scalafmtAll)
}.value

Test / test := (Test / test)
  .dependsOn(Compile / scalafmtCheck)
  .dependsOn(Test / scalafmtCheck)
  .value
