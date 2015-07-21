lazy val root = (project in file(".")).
  settings(
    organization := "com.typesafe.spark",
    name := "simple-streaming-app",
    version := "0.2.6",
    scalaVersion := "2.10.5",
    resolvers += "bintray-typesafe-maven-releases" at "http://dl.bintray.com/skyluc/maven",
    resolvers += "bintray-skyluc-maven" at "http://dl.bintray.com/skyluc/maven",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "1.4.0-streaming-t008" % "provided",
      "org.apache.spark" %% "spark-streaming" % "1.4.0-streaming-t008" % "provided",
      "com.typesafe.spark" %% "toy-rs-tcp" % "0.0.1",
      "com.github.scopt" %% "scopt" % "3.3.0"
    )
  )
