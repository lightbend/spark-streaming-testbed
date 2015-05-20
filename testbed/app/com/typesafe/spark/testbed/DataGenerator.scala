package com.typesafe.spark.testbed

import akka.actor.ActorSystem
import scala.annotation.tailrec

case class DataAtTime(time: Long, values: List[Int]) extends Ordered[DataAtTime] {
  override def compare(that: DataAtTime): Int = {
    (that.time - time).toInt
  }
  
  /** Returns a new instance with the time shifted of the given value.
   */
  def shiftTime(shift: Long) = this.copy(time = time + shift)
}

class DataGenerator(testPlan: TestPlan) {

  def valuesFor(second: Int): List[DataAtTime] = {

    currentPhase(second) match {
      case Some((phase, secondInPhase)) =>
        val secondsInPreviousPhases = second - secondInPhase
        phase.valuesFor(secondInPhase).map(_.shiftTime(secondsInPreviousPhases * 1000))
      case None =>
        List()
    }
  }

  def isDoneAt(second: Int): Boolean = {
    testPlan.duration.map { _ <= second }.getOrElse(false)
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

    loop(testPlan.phases, second)
  }

}