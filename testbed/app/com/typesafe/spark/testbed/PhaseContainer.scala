package com.typesafe.spark.testbed

import scala.annotation.tailrec

/**
 * @author luc
 */
trait PhaseContainer {
  
  val phases: List[TestPhase]
  
  def valuesFor(second: Int): List[DataAtTime] = {

    currentPhase(second) match {
      case Some((phase, secondInPhase)) =>
        val secondsInPreviousPhases = second - secondInPhase
        phase.valuesFor(secondInPhase).map(_.shiftTime(secondsInPreviousPhases * 1000))
      case None =>
        List()
    }
  }

  lazy val phasesDuration: Option[Int] = {
    if (phases.exists{_.duration.isEmpty}) {
      None
    } else {
      Some(phases.flatMap { _.duration }.sum)
    }
  }

  private def currentPhase(second: Int): Option[(TestPhase, Int)] = {
    @tailrec
    def loop(phases: List[TestPhase], remainingSecond: Int): Option[(TestPhase, Int)] = {
      phases match {
        case head :: tail =>
          head.duration match {
            case None =>
              Some(head, remainingSecond)
            case Some(duration) if duration > remainingSecond =>
              Some(head, remainingSecond)
            case Some(duration) =>
              loop(tail, remainingSecond - duration)
          }
        case Nil =>
          None
      }
    }

    loop(phases, second)
  }

}