package com.typesafe.spark.testbed

import play.api.Application
import play.api.Plugin
import play.api.libs.concurrent.Akka
import akka.actor.ActorSystem

/**
 * Lookup for actors used by the web front end.
 */
object Actors {

  private def actors(implicit app: Application): Actors = app.plugin[Actors]
    .getOrElse(sys.error("Actors plugin not registered"))

  def dataGenerator(implicit app: Application) = actors.dataGenerator
}

class Actors(app: Application) extends Plugin {

  private def system: ActorSystem= Akka.system(app)

  private lazy val dataGenerator = system.actorOf(DataGeneratorActor.props(scheduler), "dataGenerator")
  
  private lazy val serverManager = system.actorOf(ServerManagerActor.props(), "serverManager")
  
  private lazy val scheduler = system.actorOf(EpochSchedulerActor.props(serverManager), "scheduler")

  override def onStart() {
    serverManager ! ServerManagerActor.StartMsg 
  }

  override def onStop() {
    dataGenerator ! DataGeneratorActor.StopMsg
    serverManager ! ServerManagerActor.StopMsg 
  }
}
