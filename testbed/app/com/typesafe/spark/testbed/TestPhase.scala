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
    "cycle" -> CyclePhase,
    "fixed" -> FixedPhase,
    "ramp" -> RampPhase,
    "loop" -> LoopPhase,
    "noop" -> NoopPhase)
}

trait TestPhaseParser {
  def parse(config: Config): TestPhase
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

