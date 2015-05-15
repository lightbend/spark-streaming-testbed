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
    val conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
    val ssc = new StreamingContext(conf, Seconds(5))

    val lines = ssc.socketTextStream("localhost", 2222)

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

  /** Returns count, sum, mean and standard deviation
   *
   */
  private def stats(value: Iterable[Long]): (Double, Double, Double, Double) = {
    val (count, sum, sqrsum) = value.foldLeft((0L, 0L, 0L)) { (acc, v) =>
      (acc._1 + 1, acc._2 + v, acc._3 + v * v)
    }
    val mean = sum.toDouble / count
    val stddev = math.sqrt(count * sqrsum - sum * sum) / count
    (count, sum, mean, stddev)
  }

}