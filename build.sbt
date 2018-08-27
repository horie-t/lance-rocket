// See License.HORIE_Tetsuya for license details.
// See LICENSE.SiFive for license details.

organization := "io.github.horie-t"
name := "lance-rocket"
version := "0.1.0"

lazy val commonSettings = Seq(
  scalaVersion := "2.11.12",  // rocket-chipのscalaVersionに合わせる事
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xfatal-warnings",
    "-language:reflectiveCalls"
  )
)

// A RootProject (not well-documented) tells sbt to treat the target directory
// as its own root project, reading its build settings. If we instead used the
// normal `project in file()` declaration, sbt would ignore all of rocket-chip's
// build settings, and therefore not understand that it has its own dependencies
// on chisel, etc.
lazy val rocketChip = RootProject(file("rocket-chip"))

lazy val lanceRocket = (project in file(".")).
  dependsOn(rocketChip).
  settings(commonSettings: _*)
