package com.typesafe.spark.testbed

import com.typesafe.config.Config

case class CyclePhase(values: List[Int], rate: Int, duration: Option[Int]) extends TestPhase {

  def valuesFor(second: Int): List[DataAtTime] = {
    if (duration.map(_ <= second).getOrElse(false)) {
      // not part of this phase anymore, should not have been asked
      List()
    } else {
      val rateBy10ms = rate / 100D // by 10 ms
      var elementCount = 0
      (0 until 100).flatMap { i =>
        val inBucket = ((i + 1) * rateBy10ms).toInt - (i * rateBy10ms).toInt
        val res = if (inBucket == 0) {
          None
        } else {
          Some(DataAtTime(second * 1000L + i * 10, List.tabulate(inBucket)((x) => values((elementCount + x) % values.size))))
        }
        elementCount += inBucket
        res
      }(collection.breakOut)
    }
  }

}

object CyclePhase extends TestPhaseParser {

  import TestPhase._
  import scala.collection.JavaConverters._

  val KEY_VALUES = "values"
  val KEY_RATE = "rate"

  def parse(config: Config): CyclePhase = {

    val values = config.getIntList(KEY_VALUES)
    val rate = config.getInt(KEY_RATE)

    val duration = if (config.hasPath(KEY_DURATION)) {
      Some(config.getInt(KEY_DURATION))
    } else {
      None
    }

    CyclePhase(values.asScala.toList.map(_.intValue), rate, duration)
  }

}

