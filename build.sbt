lazy val root =
  project
    .in(file("."))
    .settings(name := "SCLT")
    .settings(Common.settings)
    .aggregate(utl)

lazy val utl =
  module("core")
    .settings(mainClass in Compile := Some("edu.dsfn.Main"))
    .settings(libraryDependencies ++= Common.dependencies)
    .settings(libraryDependencies ++= Dependencies.refined)
    .settings(libraryDependencies ++= Dependencies.pureConfig)
    .settings(libraryDependencies += Dependencies.fs2)
    .settings(libraryDependencies += Dependencies.wavesJ)
    .settings(mainClass in Compile := Some("edu.dsfn.Main"))

def module(modName: String) =
  Project(modName, file(s"modules/$modName"))
    .settings(name := modName)
    .settings(Common.settings)
