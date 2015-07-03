package com.typesafe.spark.testbed

import com.typesafe.config.Config

case class LoopPhase(times: Option[Int], phases: List[TestPhase]) extends TestPhase with PhaseContainer {

  lazy val duration = times.flatMap { t =>
    phasesDuration.map(_ * t)
  }

  override def valuesFor(second: Int): List[DataAtTime] = {
    if (duration.map(_ < second).getOrElse(false)) {
      // not part of this phase anymore, should not have been asked
      List()
    } else {
      val secondInLoop =
        phasesDuration
          .map(second % _)   // the position in the loop
          .getOrElse(second) // the phases in the loop don't have a finite duration.
                             // It is not the more useful situation, but it is allowed.
      val secondsBeforeCurrentLoop = phasesDuration
        .map(d => (second / d) * d)
        .getOrElse(0)
      super.valuesFor(secondInLoop)
        .map { dat =>
          dat.copy(time = dat.time + secondsBeforeCurrentLoop * 1000)
        }
    }
  }

}

object LoopPhase extends TestPhaseParser {
  
	val KEY_TIMES = "times"
			val KEY_PHASES= "phases"

  def parse(config: Config): LoopPhase = {

    val times = if (config.hasPath(KEY_TIMES)) {
      Some(config.getInt(KEY_TIMES))
    } else {
      None
    }

    import collection.JavaConverters._
    val phases: List[TestPhase] = config.getConfigList(KEY_PHASES)
      .asScala.map { config =>
        TestPhase.parse(config)
      }(collection.breakOut)

    LoopPhase(times, phases)
  }
}