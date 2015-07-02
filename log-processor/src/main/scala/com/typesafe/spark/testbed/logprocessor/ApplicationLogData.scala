package com.typesafe.spark.testbed.logprocessor

import java.text.SimpleDateFormat

case class TickLogData(time: Long, count: Int) extends LogData[TickLogData] {
  def toCSVRow: String = s"$time $count"
  
    def timeShift(shift: Long) = copy(time = time - shift)
}

case class DroppedValuesLogData(time: Long, count: Int) extends LogData[DroppedValuesLogData] {
  def toCSVRow: String = s"$time $count"
  
    def timeShift(shift: Long) = copy(time = time - shift)
}

object ApplicationLogData {
  
  private val dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ")
  
  private val tickRegex = "([^ ]* [^ ]*).*DataGeneratorActor (\\d*).*".r
  private val droppedValuesRegex = "([^ ]* [^ ]*).*to deliver (\\d*).*".r
  
  def parseTick(line: String): TickLogData = {
    line match {
      case tickRegex(date, count) =>
        TickLogData(dateParser.parse(date).getTime, count.toInt)
    }
  }
  
  def parseDroppedValues(line: String): DroppedValuesLogData = {
    line match {
      case droppedValuesRegex(date, count) =>
        DroppedValuesLogData(dateParser.parse(date).getTime, count.toInt)
    }
  }

}