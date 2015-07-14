lazy val root = (project in file(".")).
  settings(
    organization := "com.typesafe.spark",
    name := "simple-streaming-app",
    version := "0.2.5",
    scalaVersion := "2.10.5",
    resolvers += "bintray-typesafe-maven-releases" at "http://dl.bintray.com/skyluc/maven",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "1.4.0-streaming-t007" % "provided",
      "org.apache.spark" %% "spark-streaming" % "1.4.0-streaming-t007" % "provided",
      "com.github.scopt" %% "scopt" % "3.3.0"
    )
  )
