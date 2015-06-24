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

class DataGenerator(testPlan: TestPlan) extends PhaseContainer {

  val phases = testPlan.phases

  def isDoneAt(second: Int): Boolean = {
    phasesDuration.map { _ <= second }.getOrElse(false)
  }

}