package com.typesafe.spark.testbed.logprocessor

import java.text.SimpleDateFormat

case class TickLogData(time: Long, value: Int, count: Int) extends LogData[TickLogData] {
  def toCSVRow: String = s"$time $count"

  def timeShift(shift: Long) = copy(time = time - shift)
}

case class TickMultipleItemsLogData(time: Long, counts: List[Option[Int]]) extends MultipleItemsLogData[TickMultipleItemsLogData] {

  def toCSVRow: String = s"$time ${accCountsWithMissing.mkString(" ")}"

  def timeShift(shift: Long) = copy(time = time - shift)
}

case class TickMultipleValuesData(values: List[Int], entries: List[TickMultipleItemsLogData])

object TickMultipleValuesData {

  def apply(entries: List[TickLogData]): TickMultipleValuesData = {
    val values = entries.map(_.value).distinct.sorted

    val ee: List[TickMultipleItemsLogData] = entries.groupBy { _.time }.map { t => t._2 }.map { l =>
      val counts = values.map { i => l.find { _.value == i }.map { _.count } }
      val head = l.head
      TickMultipleItemsLogData(head.time, counts)
    }(collection.breakOut)

    TickMultipleValuesData(values, ee.sortBy(_.time))
  }

}

case class DroppedValuesLogData(time: Long, count: Int, clientId: Int) extends LogData[DroppedValuesLogData] {
  def toCSVRow: String = s"$time $count"

  def timeShift(shift: Long) = copy(time = time - shift)
}

case class RequestedValuesLogData(time: Long, count: Int, clientId: Int) extends LogData[RequestedValuesLogData] {
  def toCSVRow: String = s"$time $count"

  def timeShift(shift: Long) = copy(time = time - shift)
}

object ApplicationLogData {

  private val dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ")

  private val tickRegex = "([^ ]* [^ ]*).*DataGeneratorActor.*, (\\d*) times (\\d*)".r
  private val droppedValuesRegex = "([^ ]* [^ ]*).*to deliver (\\d*) values to client (\\d*)".r
  private val requestedValuesRegex = "([^ ]* [^ ]*).*received request for (\\d*) values from client (\\d*)".r

  def parseTick(line: String): TickLogData = {
    line match {
      case tickRegex(date, count, value) =>
        TickLogData(dateParser.parse(date).getTime, value.toInt, count.toInt)
    }
  }

  def parseDroppedValues(line: String): DroppedValuesLogData = {
    line match {
      case droppedValuesRegex(date, count, client) =>
        DroppedValuesLogData(dateParser.parse(date).getTime, count.toInt, client.toInt)
    }
  }

  def parseRequestedValues(line: String): RequestedValuesLogData = {
    line match {
      case requestedValuesRegex(date, count, client) =>
        RequestedValuesLogData(dateParser.parse(date).getTime, count.toInt, client.toInt)
    }
  }

}