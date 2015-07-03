package com.typesafe.spark.testbed

import org.specs2.mutable._
import org.specs2.matcher._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class RampPhaseSpec extends Specification {

  "Ramp phase" should {

    "produce constant output if startRate and endRate are equal" in {
      val phase = RampPhase(12, 25, 25, Some(4))
      def expected(second: Int): List[DataAtTime] = (0 until 25).map(t =>
        DataAtTime(second * 1000 + t * 40 + 30, List(12)))(collection.breakOut)
      
      forall(0 until 4) {second =>
    	  phase.valuesFor(second) must equalTo(expected(second))
      }
    }
    
    "use startRate, if duration is 1" in {
      val phase = RampPhase(12, 20, 25, Some(1))
      val expected: List[DataAtTime] = (0 until 20).map(t =>
        DataAtTime(t * 50 + 40, List(12)))(collection.breakOut)
      phase.valuesFor(0) must equalTo(expected)
    }

    "use startRate and endRate, if duration is 2" in {
      val phase = RampPhase(12, 10, 20, Some(2))
      val expected0: List[DataAtTime] = (0 until 10).map(t =>
        DataAtTime(t * 100 + 90, List(12)))(collection.breakOut)
      val expected1: List[DataAtTime] = (0 until 20).map(t =>
        DataAtTime(1000 + t * 50 + 40, List(12)))(collection.breakOut)
      phase.valuesFor(0) must equalTo(expected0)
      phase.valuesFor(1) must equalTo(expected1)
    }
    
    "use startRate, midpoint and endRate, if duration is 3" in {
      val phase = RampPhase(12, 10, 40, Some(3))
      val expected0: List[DataAtTime] = (0 until 10).map(t =>
        DataAtTime(t * 100 + 90, List(12)))(collection.breakOut)
      val expected1: List[DataAtTime] = (0 until 25).map(t =>
        DataAtTime(1000 + t * 40 + 30, List(12)))(collection.breakOut)
      val expected2: List[DataAtTime] = (0 until 40).map(t =>
        DataAtTime(2000 + t * 25 + (if (t * 25 % 10 == 0) 20 else 15) , List(12)))(collection.breakOut)
      phase.valuesFor(0) must equalTo(expected0)
      phase.valuesFor(1) must equalTo(expected1)
      phase.valuesFor(2) must equalTo(expected2)
    }
    
    "return no data if asked for values later than duration" in {
      val phase = RampPhase(12, 5, 33, Some(6))
      phase.valuesFor(6) must equalTo(Nil)
    }
    
    "should ramp-up lineary" in {
      val phase = RampPhase(20, 12, 72, Some(6))
      forall(0 until 6) { second =>
        phase.valuesFor(second).size must equalTo(second * 12 + 12)
        }
    }
    
    "should ramp-down lineary" in {
      val phase = RampPhase(20, 72, 12, Some(6))
      forall(0 until 6) { second =>
        phase.valuesFor(second).size must equalTo((5 - second) * 12 + 12)
        }
    }

  }

}