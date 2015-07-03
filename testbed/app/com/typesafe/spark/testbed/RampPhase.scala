package com.typesafe.spark.testbed

import com.typesafe.config.Config

case class RampPhase(value: Int, startRate: Int, endRate: Int, duration: Option[Int]) extends TestPhase {

  require(duration.isDefined, "Ramp phase has to have a duration")

  def valuesFor(second: Int): List[DataAtTime] = {
    val d = duration.get

    if (d <= second) {
      // not part of this phase anymore, should not have been asked
      List()
    } else {
      val rateForThisSecond: Double = if (d == 1) {
        startRate
      } else {
        startRate + (endRate - startRate) / (d - 1D) * second
      }
      val rateBy10ms = rateForThisSecond / 100D // by 10 ms
      (0 until 100).flatMap { i =>
        val inBucket = ((i + 1) * rateBy10ms).toInt - (i * rateBy10ms).toInt
        if (inBucket == 0) {
          None
        } else {
          Some(DataAtTime(second * 1000L + i * 10, List.fill(inBucket)(value)))
        }
      }(collection.breakOut)
    }
  }
}

object RampPhase extends TestPhaseParser {
  import TestPhase._
  val KEY_VALUE = "value"
  val KEY_START_RATE = "startRate"
  val KEY_END_RATE = "endRate"

  def parse(config: Config): RampPhase = {
    val value = config.getInt(KEY_VALUE)
    val startRate = config.getInt(KEY_START_RATE)
    val endRate = config.getInt(KEY_END_RATE)

    // ramp has to have a duration
    val duration = Some(config.getInt(KEY_DURATION))

    RampPhase(value, startRate, endRate, duration)
  }
}
