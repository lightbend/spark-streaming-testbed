enablePlugins(JavaAppPackaging)

lazy val root = (project in file(".")).
  settings(
    organization := "com.typesafe.spark",
    name := "log-processor",
    version := "0.1.0",
    scalaVersion := "2.10.5"
  )
