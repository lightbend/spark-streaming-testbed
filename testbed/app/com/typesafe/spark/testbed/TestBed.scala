package com.typesafe.spark.testbed

import java.io.File
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import akka.util.Timeout

object TestBed {

  private def usageAndExit(message: String): Nothing = {
    println(s"""$message
Usage:
  TestBed </path/to/testplan> <streaming_port>""")
    System.exit(1)
    throw new Exception("Never reached")
  }

  def main(args: Array[String]): Unit = {

    val (testPlanFile, port, reactivePort) = parseArgs(args)

    val testPlan = TestPlan.parse(testPlanFile)

    val system = ActorSystem("testbed")

    val serverManager = system.actorOf(ServerManagerActor.props(port, reactivePort), "serverManager")
    val scheduler = system.actorOf(EpochSchedulerActor.props(serverManager), "scheduler")
    val dataGenerator = system.actorOf(DataGeneratorActor.props(scheduler), "dataGenerator")

    serverManager ! ServerManagerActor.StartMsg

    import akka.pattern.ask
    implicit val timeout = Timeout(180.days)
    ask(dataGenerator, DataGeneratorActor.TestPlanMsg(testPlan)).mapTo[DataGeneratorActor.TestPlanDoneMsg]
      .map { msg =>
        println("Test plan read fully. Wait a couple of second for all data to be transmitted")
        Thread.sleep(2000)
        system.shutdown()
        System.exit(msg.status)
      }

  }

  private def parseArgs(args: Array[String]): (File, Int, Int) = {
    if (args.size < 3) {
      usageAndExit("Missing parameters")
    } else if (args.size > 3) {
      usageAndExit("Too many parameters")
    } else {
      val testPlanFile = new File(args(0))
      if (!testPlanFile.exists() || !testPlanFile.isFile()) {
        usageAndExit(s"${testPlanFile.getAbsolutePath} doesn't exist, or is not a file.")
      }
      try {
        val port = args(1).toInt
        val reactivePort = args(2).toInt
        if (port < 1 || port > 65535) {
          usageAndExit(s"${port} is not a valid port")
        } else if (reactivePort < 1 || reactivePort > 65535) {
          usageAndExit(s"${reactivePort} is not a valid port")
        } else {
          (testPlanFile, port, reactivePort)
        }
      } catch {
        case e: NumberFormatException =>
          usageAndExit(s"${args(1)} is not a valid port")
      }
    }
  }
}