package com.typesafe.spark.testbed

import com.typesafe.config.Config

trait TestPhase {

  def duration: Option[Int]

  def valuesFor(second: Int): List[DataAtTime]

}

object TestPhase {

  val KEY_TYPE = "type"
  val KEY_DURATION = "duration"

  def parse(config: Config): TestPhase = {
    val tpe = config.getString(KEY_TYPE)
    phases(tpe).parse(config)
  }

  private val phases: Map[String, TestPhaseParser] = Map(
    "fixed" -> FixedPhase,
    "noop" -> NoopPhase)
}

trait TestPhaseParser {
  def parse(config: Config): TestPhase
}

case class FixedPhase(value: Int, rate: Int, duration: Option[Int]) extends TestPhase {

  def valuesFor(second: Int): List[DataAtTime] = {
    if (duration.map(_ < second).getOrElse(false)) {
      // not part of this phase anymore, should not have been asked
      List()
    } else {
      val rateBy10ms = rate / 100D // by 10 ms
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

object FixedPhase extends TestPhaseParser {

  import TestPhase._

  val KEY_VALUE = "value"
  val KEY_RATE = "rate"

  def parse(config: Config): FixedPhase = {

    val value = config.getInt(KEY_VALUE)
    val rate = config.getInt(KEY_RATE)

    val duration = if (config.hasPath(KEY_DURATION)) {
      Some(config.getInt(KEY_DURATION))
    } else {
      None
    }

    FixedPhase(value, rate, duration)
  }

}

case class NoopPhase(duration: Option[Int]) extends TestPhase {
  def valuesFor(second: Int): List[DataAtTime] =
    Nil
}

object NoopPhase extends TestPhaseParser {

  import TestPhase._

  def parse(config: Config): NoopPhase = {

    val duration = if (config.hasPath(KEY_DURATION)) {
      Some(config.getInt(KEY_DURATION))
    } else {
      None
    }

    NoopPhase(duration)
  }
}