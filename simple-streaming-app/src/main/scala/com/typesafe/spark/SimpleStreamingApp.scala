package com.typesafe.spark

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import scala.util.Try
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.StdIn

object SimpleStreamingApp {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
    val ssc = new StreamingContext(conf, Seconds(5))
    
    val lines = ssc.socketTextStream("localhost", 2222)
    
    val numbers = lines.flatMap { line => Try(Integer.parseInt(line)).toOption }
    
    numbers.foreachRDD{rdd =>
      val sum = if (rdd.isEmpty()) {
        0
      } else {
        rdd.sum
      }
      println(s"count: ${rdd.count()}, sum: $sum")
    }
    
    ssc.start()
    
    Future {
      StdIn.readLine()
      ssc.stop(true)
    }
    
    ssc.awaitTermination()
    System.exit(0)
  }

}