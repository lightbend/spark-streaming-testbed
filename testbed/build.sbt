//name := "spark-streaming-testbed"

//version := "0.1"

//lazy val root = (project in file(".")).enablePlugins(PlayScala)

//scalaVersion := "2.11.1"

//libraryDependencies ++= Seq(
//  jdbc,
//  anorm,
//  cache,
//  ws
//)

lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  settings(
    organization := "com.typesafe.spark",
    name := "spark-streaming-testbed",
    version := "0.1",
    scalaVersion := "2.11.6",
    libraryDependencies ++= Seq(
      jdbc,
      anorm,
      cache,
      ws
    )
  )
