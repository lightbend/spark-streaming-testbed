lazy val root = (project in file(".")).
  settings(
    organization := "com.typesafe.spark",
    name := "simple-streaming-app",
    version := "0.1.6",
    scalaVersion := "2.10.5",
    resolvers += "bintray-typesafe-maven-releases" at "http://dl.bintray.com/typesafe/maven-releases",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "1.4.0",
      "org.apache.spark" %% "spark-streaming" % "1.4.0",
      "com.github.scopt" %% "scopt" % "3.3.0"
    )
  )
