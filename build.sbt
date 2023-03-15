lazy val baseName = "scala-wiremock-api"

scalaVersion := "2.13.8"

val circeVersion = "0.14.2"
val sttpVersion: String = "3.8.11"

Test / parallelExecution := false

libraryDependencies ++= List(
  "com.github.tomakehurst" % "wiremock" % "2.27.2",
  "ch.qos.logback" % "logback-classic" % "1.4.6",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
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
    case Some((2, _)) => Seq("-Ytasty-reader") // flags only needed in Scala 2
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
