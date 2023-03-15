lazy val baseName = "scala-wiremock-api"

scalaVersion := "2.13.8"

libraryDependencies ++= List(
  "com.github.tomakehurst" % "wiremock" % "2.27.2",
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)

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
