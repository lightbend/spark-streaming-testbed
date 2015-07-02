package com.typesafe.spark.testbed.logprocessor

import java.text.SimpleDateFormat

case class FeedbackLogData(time: Long, limit: Int) extends LogData[FeedbackLogData] {
  def toCSVRow: String = s"$time $limit"
  
    def timeShift(shift: Long) = copy(time = time - shift)
}

case class RatioLogData(time: Long, ratio: Double) extends LogData[RatioLogData] {
  def toCSVRow: String = s"$time $ratio"
  
    def timeShift(shift: Long) = copy(time = time - shift)
}

object ReceiverLogData {
  
  private val dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSZ")
  
  private val feedbackRegex = "([^ ]* [^ ]*).*update for.*: (\\d*)".r
  private val ratioRegex = "([^ ]* [^ ]*).*with ratio of ([\\d.]*)\\.".r
  
  def parseFeedback(line: String): FeedbackLogData = {
    line match {
      case feedbackRegex(date, limit) =>
        FeedbackLogData(dateParser.parse(date).getTime, limit.toInt)
    }
  }
  
  def parseRatio(line: String): RatioLogData = {
    line match {
      case ratioRegex(date, ratio) =>
        RatioLogData(dateParser.parse(date).getTime, ratio.toDouble)
    }
  }

}