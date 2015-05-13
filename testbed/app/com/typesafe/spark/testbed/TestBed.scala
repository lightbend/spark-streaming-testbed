package com.typesafe.spark.testbed

import java.io.File
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import akka.util.Timeout

object TestBed {

  private def usageAndExit1(message: String) {
    println(s"""$message
Usage:
  TestBed </path/to/testplan>""")
    System.exit(1)
  }

  def main(args: Array[String]): Unit = {
    if (args.size != 1) {
      usageAndExit1("Missing testplan file.")
    }
    val testPlanFile = new File(args(0))
    if (!testPlanFile.exists() || !testPlanFile.isFile()) {
      usageAndExit1(s"${testPlanFile.getAbsolutePath} doesn't exist, or is not a file.")
    }

    val testPlan = TestPlan.parse(testPlanFile)

    val system = ActorSystem("testbed")

    val serverManager = system.actorOf(ServerManagerActor.props(), "serverManager")
    val scheduler = system.actorOf(EpochSchedulerActor.props(serverManager), "scheduler")
    val dataGenerator = system.actorOf(DataGeneratorActor.props(scheduler), "dataGenerator")

    serverManager ! ServerManagerActor.StartMsg

    import akka.pattern.ask
    implicit val timeout = Timeout(180 days)
    ask(dataGenerator, DataGeneratorActor.TestPlanMsg(testPlan)).mapTo[DataGeneratorActor.TestPlanDoneMsg]
      .map { msg =>
        println("Test plan read fully. Wait a couple of second for all data to be transmitted")
        Thread.sleep(2000)
        system.shutdown()
        System.exit(msg.status)
      }

  }

}