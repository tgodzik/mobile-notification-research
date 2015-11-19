lazy val commonSettings = Seq(
  organization := "pl.edu.agh.mobilne.modbustest.app",
  version := "0.1.0",
  scalaVersion := "2.11.7"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "intro",
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"
  )
