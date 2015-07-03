package com.typesafe.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Milliseconds
import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io
import com.typesafe.spark.test.Hanoi
import org.apache.spark.storage.StorageLevel

object SimpleStreamingApp {

  def main(args: Array[String]): Unit = {

    val (hostname, port, strategy, batchInterval) = parseArgs(args)

    val conf = new SparkConf()
      .setAppName("Streaming tower of Hanoi resolution")
      .set("spark.streaming.receiver.congestionStrategy", strategy)

    val ssc = new StreamingContext(conf, Milliseconds(batchInterval))

    val lines = ssc.socketTextStream(hostname, port, StorageLevel.MEMORY_ONLY)

    val numbers = lines.flatMap { line => Try(Integer.parseInt(line)).toOption }

    val hanoiTime = numbers.map { i =>
      // keep track of time to compute
      val startTime = System.currentTimeMillis()

      // resolve the tower of Hanoi
      Hanoi.solve(i)

      val executionTime = System.currentTimeMillis() - startTime
      (i, executionTime)
    }

    val statsByValues = hanoiTime.groupByKey().mapValues { stats }

    statsByValues.foreachRDD { (v, time) =>
      if (!v.isEmpty()) {
        v.collect.foreach(s => println(format(time.milliseconds, s)))
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

  private def format(batchTime: Long, stats: (Int, (Int, Long, Double, Double, Long))): String = {
    s"batch result: ${stats._2._5}\t$batchTime\t${stats._1}\t${stats._2._1}\t${stats._2._2}\t${stats._2._3}\t${stats._2._4}"
  }

  /**
   * Returns count, sum, mean and standard deviation
   *
   */
  private def stats(value: Iterable[Long]): (Int, Long, Double, Double, Long) = {
    val (count, sum, sqrsum) = value.foldLeft((0, 0L, 0L)) { (acc, v) =>
      // acc: count, sum, sum of squared
      (acc._1 + 1, acc._2 + v, acc._3 + v * v)
    }
    val mean = sum.toDouble / count
    val stddev = math.sqrt(count * sqrsum - sum * sum) / count
    (count, sum, mean, stddev, System.currentTimeMillis())
  }

  private def usageAndExit(message: String): Nothing = {
    println(s"""$message
Usage:
  SimpleStreamingApp <stream_hostname> <stream_port> <congestion strategy> <batch interval milliseconds>""")
    System.exit(1)
    throw new Exception("Never reached")
  }

  private def parseArgs(args: Array[String]): (String, Int, String, Int) = {
    if (args.size < 4) {
      usageAndExit("Missing parameters")
    } else if (args.size > 4) {
      usageAndExit("Too many parameters")
    } else {
      val hostname = args(0)
      val strategy = args(2)
      if (!List("ignore", "drop", "sampling", "pushback", "reactive").contains(strategy))
        usageAndExit(s"${args(2)} is not a valid strategy")
      val port = Try(args(1).toInt) recover {
        case e: NumberFormatException =>
          usageAndExit(s"${args(1)} is not a valid port")
      }
      val batchInterval = Try(args(3).toInt) recover {
        case e: NumberFormatException =>
          usageAndExit(s"${args(3)} is not a valid batch interval")
      }
      if (port.get < 1 || port.get > 65535) {
        usageAndExit(s"${args(1)} is not a valid port")
      } else {
        (hostname, port.get, strategy, batchInterval.get)
      }
    }
  }
}
