package com.typesafe.spark.testbed

import akka.actor.ActorSystem
import scala.annotation.tailrec

class DataGenerator(testPlan: TestPlan) {
  
  def valuesFor(second: Int): List[(Int, Long)] = {
    
    currentPhase(second) match {
      case Some((phase, secondInPhase)) =>
        val secondsInPreviousPhases = second - secondInPhase
        phase.valuesFor(secondInPhase).map(t => (t._1, t._2 + secondsInPreviousPhases * 1000))
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