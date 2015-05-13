package com.typesafe.spark.testbed

import com.typesafe.config.ConfigFactory
import java.io.File
import com.typesafe.config.Config

case class TestPlan(phases: List[TestPhase]) {
  lazy val duration: Option[Int] = {
    phases.map(_.duration).foldLeft(Some(0): Option[Int]) { (acc, d) =>
      acc.flatMap { x => d.map(_ + x) }
    }
  }
}

object TestPlan {

  val KEY_SEQUENCE = "sequence"

  def parse(plan: File): TestPlan = {
    parse(ConfigFactory.parseFile(plan))
  }

  def parse(plan: String): TestPlan = {
    parse(ConfigFactory.parseString(plan))
  }

  private def parse(plan: Config): TestPlan = {
    val sequenceArray = plan.getConfigList(KEY_SEQUENCE)

    import collection.JavaConverters._
    val phases: List[TestPhase] = sequenceArray.asScala.map {
      TestPhase.parse(_)
    }(collection.breakOut)

    TestPlan(phases)
  }

}