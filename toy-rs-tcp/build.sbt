lazy val root = (project in file(".")).
  settings(
    organization := "com.typesafe.spark",
    name := "toy-rs-tcp",
    version := "0.0.1",
    scalaVersion := "2.10.5",
    libraryDependencies ++= Seq(
      "org.reactivestreams" % "reactive-streams" % "1.0.0",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test"
    ),
    bintrayOrganization := Some("skyluc"),
    bintrayReleaseOnPublish := false,
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))
  )
