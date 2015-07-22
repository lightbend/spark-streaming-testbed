package com.typesafe.spark.testbed

import play.api.Application
import play.api.Plugin
import play.api.libs.concurrent.Akka
import akka.actor.ActorSystem

/**
 * Lookup for actors used by the web front end.
 * Actors are creating through this mechanism only when running the test as a Play application.
 * Check TestBed for manual actor initialization.
 */
object Actors {

  private def actors(implicit app: Application): Actors = app.plugin[Actors]
    .getOrElse(sys.error("Actors plugin not registered"))

  def dataGenerator(implicit app: Application) = actors.dataGenerator
}

class Actors(app: Application) extends Plugin {

  private def system: ActorSystem = Akka.system(app)

  private def socketPort = app.configuration.getInt("testbed.port").getOrElse(2222)

  private def reactiveSocketPort = app.configuration.getInt("testbed.reactiveport").getOrElse(2223)

  private lazy val dataGenerator = system.actorOf(DataGeneratorActor.props(scheduler), "dataGenerator")

  private lazy val serverManager = system.actorOf(ServerManagerActor.props(socketPort, reactiveSocketPort), "serverManager")

  private lazy val scheduler = system.actorOf(EpochSchedulerActor.props(serverManager), "scheduler")

  override def onStart() {
    serverManager ! ServerManagerActor.StartMsg
  }

  override def onStop() {
    dataGenerator ! DataGeneratorActor.StopMsg
    serverManager ! ServerManagerActor.StopMsg
  }
}
