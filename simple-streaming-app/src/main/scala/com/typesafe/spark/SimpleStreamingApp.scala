package com.typesafe.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Milliseconds
import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.spark.test.Hanoi
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import scopt.OptionParser
import scopt.Zero

/** Simple statistics data class
 */
private case class Stats(count: Int, sum: Long, mean: Double, stdDev: Double, millis: Long)

object SimpleStreamingApp {

  def main(args: Array[String]): Unit = {

    val config = parseArgs(args)

    {
      import config._
      println(s"""
        Connecting to $hostname:$port using Spark $master and $streams receivers.
      """)
    }
    val conf = new SparkConf()
      .setAppName("Streaming tower of Hanoi resolution")
      .set("spark.streaming.receiver.congestionStrategy", config.strategy)

     if (conf.getOption("spark.master").isEmpty)
       conf.setMaster(config.master)

    val ssc = new StreamingContext(conf, Milliseconds(config.batchInterval))

    // create n receivers, each one having more elements than the previous one
    val rawInputs = for (i <- 1 to config.streams) yield
      ssc.socketTextStream(config.hostname, config.port, StorageLevel.MEMORY_ONLY)
         .flatMap(x => Seq.fill(1 + (i - 1) * config.step)(x))

    val lines = rawInputs.reduce(_ union _)

    val numbers = lines.flatMap { line => Try(Integer.parseInt(line)).toOption }

    val hanoiTime = numbers.map { i =>
      // keep track of time to compute
      val startTime = System.currentTimeMillis()

      // resolve the tower of Hanoi
      Hanoi.solve(i)

      val executionTime = System.currentTimeMillis() - startTime
      (i, executionTime)
    }

    val statsByValues: DStream[(Int, Stats)] = hanoiTime.groupByKey().mapValues { stats }

    statsByValues.foreachRDD { (v, time) =>
      if (!v.isEmpty()) {
        v.collect.foreach(s => println(format(time.milliseconds, s._1, s._2)))
      }
    }

    ssc.start()

    Future {
      while (Console.readLine() != "done") {

      }
      ssc.stop(true)
    }

    ssc.awaitTermination()
    System.exit(0)
  }

  private def format(batchTime: Long, input: Int, stats: Stats): String = {
    s"batch result: ${stats.millis}\t$batchTime\t$input\t${stats.count}\t${stats.sum}\t${stats.mean}\t${stats.stdDev}"
  }

  /** Returns count, sum, mean and standard deviation
   *
   */
  private def stats(value: Iterable[Long]): Stats = {
    val (count, sum, sqrsum) = value.foldLeft((0, 0L, 0L)) { (acc, v) =>
      // acc: count, sum, sum of squared
      (acc._1 + 1, acc._2 + v, acc._3 + v * v)
    }
    val mean = sum.toDouble / count
    val stddev = math.sqrt(count * sqrsum - sum * sum) / count
    Stats(count, sum, mean, stddev, System.currentTimeMillis())
  }

  val DefaultConfig = Config("local[*]", "", -1, 1000, "ignore", 1, 1)

  private val parser = new OptionParser[Config]("simple-streaming") {
    help("help")
      .text("Prints this usage text")

    opt[String]('h', "hostname")
      .required()
      .action { (x, c) => c.copy(hostname = x) }
      .text("Hostname where receivers should connect")

    opt[Int]('p', "port")
      .required()
      .action { (x, c) => c.copy(port = x) }
      .text("Port number to which receivers should connect")

    opt[String]('m', "master")
      .action { (x, c) => c.copy(master = x) }
      .text("Spark master to connect to")

    opt[String]('s', "strategy")
      .action { (x, c) => c.copy(strategy = x) }
      .text("Push-back strategy to use")
      .validate { s =>
        if (Set("ignore", "drop", "sampling", "pushback", "reactive").contains(s))
          success
        else
          failure(s"$s is not a valid strategy")
      }

    opt[Int]('b', "batch-interval")
      .action { (x, c) => c.copy(batchInterval = x) }
      .text("The batch interval in milliseconds.")

    opt[(Int, Int)]('r', "receivers")
      .action { (x, c) => c.copy(streams = x._1, step = x._2) }
      .text("The number of receivers and the multiplier for stream elements, separated by `=`.")

  }

  private def parseArgs(args: Array[String]): Config = {
    parser.parse(args, DefaultConfig) match {
      case Some(config) => config
      case None =>
        sys.exit(1)
    }
  }
}
