lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  settings(
    organization := "com.typesafe.spark",
    name := "spark-streaming-testbed",
    version := "0.1.2",
    scalaVersion := "2.10.5",
    libraryDependencies ++= Seq(
      jdbc,
      anorm,
      cache,
      ws
    )
  )
