package com.typesafe.spark.testbed.logprocessor

import java.text.SimpleDateFormat

case class MemoryLogData(time: Long, freeMemoryKb: Double) extends LogData[MemoryLogData] {
  def toCSVRow: String = s"$time $freeMemoryKb"
  
  def timeShift(shift: Long) = copy(time = time - shift)
}

case class ExecutionLogData(time: Long, batchTime: Long, item: Int, count: Int) extends LogData[ExecutionLogData] {
  def toCSVRow: String = s"$time ${batchTime} $item $count"
  
  def timeShift(shift: Long) = copy(time = time - shift, batchTime = batchTime - shift)
}

object RunLogData {

  private val memoryRegex = "([^ ]* [^ ]*).*free: ([^ ]*) (..)\\)".r
  private val dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSZ")
  
  private val executionRegex = "[^\\d]*(\\d*)\t(\\d*)\t(\\d*)\t(\\d*).*".r

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
      case executionRegex(logTimeString, batchTimeString, itemString, countString) =>
        ExecutionLogData(logTimeString.toLong, batchTimeString.toLong, itemString.toInt, countString.toInt)
    }
  }

}