package com.typesafe.spark.testbed

import org.specs2.mutable._
import org.specs2.matcher._
import org.specs2.runner._
import org.junit.runner._

@RunWith(classOf[JUnitRunner])
class LoopPhaseSpec extends Specification {

  "Loop phase" should {

    "have infinite duration if no time is specified" in {
      val phase = LoopPhase(None, Nil)
      phase.duration must equalTo(None)
    }

    "have infinite duration if one of the inner phase has no specified time" in {
      val phase = LoopPhase(
        Some(2),
        List(
          FixedPhase(5, 4, None),
          FixedPhase(4, 5, Some(2))))
      phase.duration must equalTo(None)
    }

    "repead one phase" in {
      val phase = LoopPhase(
        Some(3),
        List(
          FixedPhase(3, 10, Some(2))))
      def expected(second: Int): List[DataAtTime] = (0 until 10).map(t =>
        DataAtTime(second * 1000 + t * 100 + 90, List(3)))(collection.breakOut)

      forall(0 to 6) { second =>
        phase.valuesFor(second) must equalTo(expected(second))
      }
    }

    "repead two phases" in {
      val phase = LoopPhase(
        Some(3),
        List(
          FixedPhase(3, 10, Some(2)),
          FixedPhase(4, 5, Some(3))))

      def expected1(second: Int): List[DataAtTime] = (0 until 10).map(t =>
        DataAtTime(second * 1000 + t * 100 + 90, List(3)))(collection.breakOut)
      def expected2(second: Int): List[DataAtTime] = (0 until 5).map(t =>
        DataAtTime(second * 1000 + t * 200 + 190, List(4)))(collection.breakOut)

      forall(0 to 15) { second =>
        val expected = if (second % 5 <= 1)
          expected1(second)
        else
          expected2(second)
        phase.valuesFor(second) must equalTo(expected)
      }
    }
  }

}