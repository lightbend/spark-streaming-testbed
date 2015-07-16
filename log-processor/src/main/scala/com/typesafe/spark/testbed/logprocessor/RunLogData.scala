package com.typesafe.spark.testbed.logprocessor

import java.text.SimpleDateFormat

case class MemoryLogData(time: Long, freeMemoryKb: Double) extends LogData[MemoryLogData] {
  def toCSVRow: String = s"$time $freeMemoryKb"

  def timeShift(shift: Long) = copy(time = time - shift)
}

case class ExecutionTimeLogData(time: Long, batchTime: Long) extends LogData[ExecutionTimeLogData] {
  def toCSVRow: String = s"$time $batchTime"

  def timeShift(shift: Long) = copy(time = time - shift, batchTime = batchTime - shift)
}

object ExecutionTimeData {
  def apply(log: List[ExecutionLogData]): List[ExecutionTimeLogData] = {
    log.map { l =>
      ExecutionTimeLogData(l.time, l.batchTime)
    }.groupBy {_.batchTime}.map(_._2.head).to[List].sortBy{ _.batchTime}
  }
}

case class ExecutionLogData(time: Long, batchTime: Long, value: Int, streamId: Int, count: Int) extends LogData[ExecutionLogData] {
  def toCSVRow: String = s"$time $batchTime $value $count"

  def timeShift(shift: Long) = copy(time = time - shift, batchTime = batchTime - shift)
}

case class ExecutionMultipleItemsLogData(time: Long, batchTime: Long, counts: List[Option[Int]]) extends MultipleItemsLogData[ExecutionMultipleItemsLogData] {

  def toCSVRow: String = s"$time $batchTime ${accCountsWithMissing.mkString(" ")}"

  def timeShift(shift: Long) = copy(time = time - shift, batchTime = batchTime - shift)
}

case class ExecutionMultipleValuesData(values: List[Int], entries: List[ExecutionMultipleItemsLogData])

object ExecutionMultipleValuesData {

  def apply(entries: List[ExecutionLogData]): ExecutionMultipleValuesData = {
    val values = entries.map(_.value).distinct.sorted

    val ee: List[ExecutionMultipleItemsLogData] = entries.groupBy { _.batchTime }.map { t => t._2 }.map { l =>
      val counts = values.map { v => l.find { _.value == v }.map { _.count } }
      val head = l.head
      ExecutionMultipleItemsLogData(head.time, head.batchTime, counts)
    }(collection.breakOut)

    ExecutionMultipleValuesData(values, ee.sortBy(_.time))
  }

}

object RunLogData {

  private val memoryRegex = "([^ ]* [^ ]*).*free: ([^ ]*) (..)\\)".r
  private val dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSZ")

  private val executionRegex = "[^\\d]*(\\d*)\t(\\d*)\t(\\d*)\t(\\d*)\t(\\d*).*".r

  def parseMemory(line: String): MemoryLogData = {
    line match {
      case memoryRegex(date, memoryValueString, memoryUnit) =>
        val memoryValue = memoryValueString.toDouble
        val memory =
          if (memoryUnit == "MB")
            memoryValue * 1024
          else
            memoryValue

        val time = dateParser.parse(date).getTime
        MemoryLogData(time, memory)
    }
  }

  def parseExecution(line: String): ExecutionLogData = {
    line match {
      case executionRegex(logTimeString, batchTimeString, itemString, streamId, countString) =>
        ExecutionLogData(logTimeString.toLong, batchTimeString.toLong, itemString.toInt, streamId.toInt, countString.toInt)
    }
  }

}