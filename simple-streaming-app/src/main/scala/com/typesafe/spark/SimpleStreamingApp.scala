package com.typesafe.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn
import com.typesafe.spark.test.Hanoi

object SimpleStreamingApp {

  def main(args: Array[String]): Unit = {

    val (sparkMaster, hostname, port) = parseArgs(args)

    val conf = new SparkConf().setMaster(sparkMaster).setAppName("NetworkWordCount")
    val ssc = new StreamingContext(conf, Seconds(5))

    val lines = ssc.socketTextStream(hostname, port)

    val numbers = lines.flatMap { line => Try(Integer.parseInt(line)).toOption }

    val hanoiTime = numbers.map { i =>
      val startTime = System.currentTimeMillis()
      Hanoi.solve(i)
      val executionTime = System.currentTimeMillis() - startTime
      (i, executionTime)
    }

    val statsByValues = hanoiTime.groupByKey().mapValues { stats(_) }

    statsByValues.foreachRDD { v =>
      if (!v.isEmpty()) {
        println("=========")
        v.foreach(println(_))
      }
    }

    ssc.start()

    Future {
      StdIn.readLine()
      ssc.stop(true)
    }

    ssc.awaitTermination()
    System.exit(0)
  }

  /**
   * Returns count, sum, mean and standard deviation
   *
   */
  private def stats(value: Iterable[Long]): (Double, Double, Double, Double) = {
    val (count, sum, sqrsum) = value.foldLeft((0L, 0L, 0L)) { (acc, v) =>
      // acc: count, sum, sum of squared
      (acc._1 + 1, acc._2 + v, acc._3 + v * v)
    }
    val mean = sum.toDouble / count
    val stddev = math.sqrt(count * sqrsum - sum * sum) / count
    (count, sum, mean, stddev)
  }

  private def usageAndExit(message: String): Nothing = {
    println(s"""$message
Usage:
  SimpleStreamingApp <spark_master> <stream_hostname> <stream_port>""")
    System.exit(1)
    throw new Exception("Never reached")
  }

  private def parseArgs(args: Array[String]): (String, String, Int) = {
    if (args.size < 3) {
      usageAndExit("Missing parameters")
    } else if (args.size > 3) {
      usageAndExit("Too many parameters")
    } else {
      val sparkMaster = args(0)
      val hostname = args(1)
      try {
        val port = args(2).toInt
        if (port < 1 || port > 65535) {
          usageAndExit(s"${args(1)} is not a valid port")
        } else {
          (sparkMaster, hostname, port)
        }
      } catch {
        case e: NumberFormatException =>
          usageAndExit(s"${args(1)} is not a valid port")
      }
    }
  }

}