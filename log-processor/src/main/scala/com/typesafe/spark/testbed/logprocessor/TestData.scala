package com.typesafe.spark.testbed.logprocessor

import java.io.File
import scala.io.Source
import java.io.FileWriter
import scala.annotation.tailrec

case class TestData(
  memory: List[MemoryLogData],
  execution: List[ExecutionLogData],
  tick: List[TickLogData],
  droppedValues: List[DroppedValuesLogData],
  feedback: List[FeedbackLogData],
  ratio: List[RatioLogData]) {

  lazy val droppedValuesPerSecond: List[DroppedValuesLogData] = {
    val unordered: List[DroppedValuesLogData] = droppedValues.groupBy { _.time / 1000 }.map { g =>
      DroppedValuesLogData(g._1 * 1000, g._2.map(_.count).sum)
    }(collection.breakOut)

    unordered.sortBy(_.time)
  }

  lazy val executionMultipleValues: ExecutionMultipleValuesData =
    ExecutionMultipleValuesData(execution)

  lazy val tickMultipleValues: TickMultipleValuesData =
    TickMultipleValuesData(tick)

  /**
   * Change the data such as time 0 is the time of the first processed batch.
   */
  def timeShift: TestData = {
    val baseTime = execution.head.batchTime
    TestData(
      memory.map(_.timeShift(baseTime)),
      execution.map(_.timeShift(baseTime)),
      tick.map(_.timeShift(baseTime)),
      droppedValues.map(_.timeShift(baseTime)),
      feedback.map(_.timeShift(baseTime)),
      ratio.map(_.timeShift(baseTime)))
  }

  /**
   * Dump the date in multiple files in the given folder.
   */
  def dump(workFolder: File): Unit = {
    workFolder.mkdirs()
    TestData.dump(memory, new File(workFolder, "memory.log"))
    TestData.dump(executionMultipleValues.entries, new File(workFolder, "execution.log"))
    TestData.dump(tickMultipleValues.entries, new File(workFolder, "tick.log"))
    TestData.dump(droppedValues, new File(workFolder, "droppedValues.log"))
    TestData.dump(droppedValuesPerSecond, new File(workFolder, "droppedValuesPerSecond.log"))
    TestData.dump(feedback, new File(workFolder, "feedback.log"))
    TestData.dump(ratio, new File(workFolder, "ratio.log"))
  }

  /**
   * Returns the minimum value for time in all the log entries.
   */
  def minTime: Long = {
    List(memory, execution, tick, droppedValues, feedback, ratio).map { l =>
      if (l.isEmpty) {
        Long.MaxValue
      } else {
        l.map(_.time).min
      }
    }.min
  }

  /**
   * Returns the maximum value for time in all the log entries.
   */
  def maxTime: Long = {
    List(memory, execution, tick, droppedValues, feedback, ratio).map { l =>
      if (l.isEmpty) {
        Long.MinValue
      } else {
        l.map(_.time).max
      }
    }.max
  }

}

/**
 * A log entry, with time, and a few helper methods.
 */
trait LogData[A] {

  def time: Long

  def toCSVRow: String
  def timeShift(shift: Long): A
}

trait MultipleItemsLogData[A] extends LogData[A] {
  def counts: List[Option[Int]]

  /**
   * Returns the string representation of the accumulated values in counts,
   *  with the missing values replaced by '?'.
   */
  def accCountsWithMissing =
    counts.foldLeft((List[String](), 0)) { (acc, v) =>
      v.map { c =>
        val a = c + acc._2
        (a.toString :: acc._1, a)
      }.getOrElse(("?" :: acc._1, acc._2))
    }._1.reverse
}

object TestData {

  /**
   * Load the data contained in the log files from the given folder.
   */
  def load(baseFolder: File): TestData = {
    val runAllLines = Source.fromFile(new File(baseFolder, "run.log")).getLines().toStream

    val runAddedInput = runAllLines
      .filter { _.contains("Added input") }
      .map(RunLogData.parseMemory(_))
      .to[List]

    val runExecution = runAllLines
      .filter { _.contains("batch result:") }
      .map(RunLogData.parseExecution(_))
      .to[List]

    val applicationAllLines = Source.fromFile(new File(baseFolder, "application.log")).getLines().toStream

    val applicationTick = applicationAllLines
      .filter { _.contains("At tick") }
      .map(ApplicationLogData.parseTick(_))
      .to[List]

    val applicationDroppedValues = applicationAllLines
      .filter { _.contains("ConnectionManagerActor") }
      .map(ApplicationLogData.parseDroppedValues(_))
      .to[List]

    val receiverAllLines = Source.fromFile(new File(baseFolder, "receiver.log")).getLines().toStream

    val receiverFeedback = receiverAllLines
      .filter(_.contains("Received update"))
      .map(ReceiverLogData.parseFeedback(_))
      .to[List]

    val receiverRatio = receiverAllLines
      .filter(_.contains("ratio of"))
      .map(ReceiverLogData.parseRatio(_))
      .to[List]

    TestData(runAddedInput, runExecution, applicationTick, applicationDroppedValues, receiverFeedback, receiverRatio)
  }

  /**
   * Write the log entries in the given file
   */
  private def dump(items: List[LogData[_]], file: File) {
    val writer = new FileWriter(file)

    @tailrec
    def loop(items: List[LogData[_]], previousTime: Long, previousInterval: Long) {
      items match {
        case head :: tail =>
          val interval = head.time - previousTime
          if (interval > previousInterval * 10)
            writer.write("\n")
          writer.write(s"${head.toCSVRow}\n")
          loop(tail, head.time, interval)
        case Nil =>
      }
    }

    loop(items, -20000, 20000)

    writer.close()
  }
}