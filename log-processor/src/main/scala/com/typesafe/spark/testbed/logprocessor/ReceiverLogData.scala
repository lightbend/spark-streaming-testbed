package com.typesafe.spark.testbed.logprocessor

import java.text.SimpleDateFormat

case class FeedbackLogData(time: Long, streamId: Int, limit: Int) extends LogData[FeedbackLogData] {
  def toCSVRow: String = s"$time $limit"
  
    def timeShift(shift: Long) = copy(time = time - shift)
}

case class RatioLogData(time: Long, streamId: Int, ratio: Double) extends LogData[RatioLogData] {
  def toCSVRow: String = s"$time $ratio"
  
    def timeShift(shift: Long) = copy(time = time - shift)
}

object ReceiverLogData {
  
  private val dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSSZ")
  
  private val feedbackRegex = "([^ ]* [^ ]*).*a new rate limit for (\\d*) : (\\d*)\\.".r
  private val ratioRegex = "([^ ]* [^ ]*).*stream (\\d*).*with ratio of ([\\d.]*)\\.".r
  
  def parseFeedback(line: String): FeedbackLogData = {
    line match {
      case feedbackRegex(date, streamId, limit) =>
        FeedbackLogData(dateParser.parse(date).getTime, streamId.toInt, limit.toInt)
    }
  }
  
  def parseRatio(line: String): RatioLogData = {
    line match {
      case ratioRegex(date, streamId, ratio) =>
        RatioLogData(dateParser.parse(date).getTime, streamId.toInt, ratio.toDouble)
    }
  }

}
