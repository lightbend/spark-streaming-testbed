lazy val root = (project in file(".")).
  settings(
    organization := "com.typesafe.spark",
    name := "simple-streaming-app",
    version := "0.1.7",
    scalaVersion := "2.10.5",
    resolvers += "bintray-typesafe-maven-releases" at "http://dl.bintray.com/typesafe/maven-releases",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "1.4.0",
      "org.apache.spark" %% "spark-streaming" % "1.4.0"
    )
  )
