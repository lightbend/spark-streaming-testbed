lazy val root = (project in file(".")).
  settings(
    organization := "com.typesafe.spark",
    name := "simple-streaming-app",
    version := "0.1",
    scalaVersion := "2.11.6",
    resolvers += "bintray-typesafe-maven-releases" at "http://dl.bintray.com/typesafe/maven-releases",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "1.3.1-typesafe-001",
      "org.apache.spark" %% "spark-streaming" % "1.3.1-typesafe-001"
    )
  )
