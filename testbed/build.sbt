lazy val root = (project in file(".")).
  enablePlugins(PlayScala).
  settings(
    organization := "com.typesafe.spark",
    name := "spark-streaming-testbed",
    version := "0.1.3",
    scalaVersion := "2.10.5",
    resolvers += "bintray-skyluc-maven" at "http://dl.bintray.com/skyluc/maven",
    libraryDependencies ++= Seq(
      jdbc,
      anorm,
      cache,
      ws,
      "com.typesafe.spark" %% "toy-rs-tcp" % "0.0.1"
    )
  )
