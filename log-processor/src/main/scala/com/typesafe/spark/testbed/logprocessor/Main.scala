package com.typesafe.spark.testbed.logprocessor

import java.io.File
import java.io.FileWriter
import scala.sys.process.Process

object Main {

  def main(args: Array[String]): Unit = {
    // TODO: check the arguments, and the availability of the log files
    val baseFolder = new File(args(0))
    val title = args(1)

    // load the data for the logs
    val data = TestData.load(baseFolder)

    // generate the graph(s)
    createGraphs(data.timeShift, title, baseFolder)
  }

  private def createGraphs(data: TestData, title: String, workFolder: File) {

    // dump in files the data needed by gnuplot
    data.dump(workFolder)

    // generate the gnuplot scripte
    val gsWriter = new FileWriter(new File(workFolder, "graph.gnuplot"))
    gsWriter.write(generateGnuplotScript(data, title))
    gsWriter.close

    // run gnuplot
    val l = Process("/usr/bin/gnuplot graph.gnuplot", workFolder).lines
    println(l.mkString)
  }

  private def generateGnuplotScript(data: TestData, title: String): String = {

    val builder = new StringBuilder("""
set y2range [0:]
set y2tics
set yrange [0:]
set lmargin 12
set rmargin 10
set datafile missing "?"
set style fill transparent solid 0.25

set style arrow 1 nohead ls 1
set ytics nomirror
""")
    builder.append(s"""
set terminal pngcairo dashed enhanced font "arial,10" fontscale 1.0 size 1500,${data.dataPerStream.size * 333 + 666}
""")
    builder.append("""
set output "graph.png"
""")
    builder.append(s"""
set multiplot layout ${data.dataPerStream.size + 2}, 1 title "$title"
""")

    builder.append(s"""
set xrange [ ${data.minTime - 5000} : ${data.maxTime + 5000} ]""")

    builder.append("""
set xtics format " "
set bmargin 1
set ylabel "execution time (in milliseconds)"
set y2label "memory (in MB)"

plot "memory.log" using 1:(5000) with line lt 0 lc 3 title "batch interval", \
  "execution.log" using 2:(0):($1-$2):($1-$2) with vector title "Spark - delay + processing, of each batch" arrowstyle 1, \
  "memory.log" using 1:($2/1024) axes x1y2 with lines title "Spark - free memory to store the blocks" lt 1 lc 2
 

""")

    builder.append("""
set tmargin 0
set ylabel "# of items"
set y2label "drop ratio"
set y2range [ 0 : 1.1 ]

""")

    var column = 3
    data.dataPerStream.foreach { stream =>

      val id = stream.streamId

      builder.append("""
plot """)

      if (!stream.ratio.isEmpty)
        builder.append(s""""ratio_${stream.streamId}.log" using 1:2 axes x1y2 with lines title "Congestion strategie - drop ratio, for each block" lt 1 lc rgb "#DDDDDD", \\
  "ratio_$id.log" u 1:2 axes x1y2 smooth bezier title "smoothed drop ratio" lt 1 lc "black", \\
""")

      val executionLines = stream.execution.values.zipWithIndex.map { t =>
        s""""execution_$id.log" using 2:($$${t._2 + 3}) with filledcurve x1 title "Spark - # of items ${t._1} processed per batch" lt 1 lc ${t._2 + 3}"""
      }
      builder.append(executionLines.mkString(""", \
"""))

      if (!stream.feedback.isEmpty)
        builder.append(s""", \\
  "feedback_$id.log" using 1:($$2 * 5) with lines title "Spark - feedback bound, max # of item per batch" lt 1 lc 2""")
      builder.append("""

""")
    }

    builder.append("""
set xlabel "timeline (in milliseconds)"

""")

    builder.append("""
set xtics format "%.0f"
set bmargin 3
set ylabel "# of items"
unset y2label
unset y2tics
unset y2range

""")

    val maxTickValue = data.tick.map(_.count).max

    builder.append(s"""
set yrange [ 0 : ${(maxTickValue * 1.2).toInt} ]
""")

    builder.append("""
set boxwidth 1000

plot "droppedValuesPerSecond.log" using 1:2 with boxes title "testbed, # of item dropped per second, as TCP socket was not ready" lt 1 lc 1, \
""")

    val tickLines = data.tickMultipleValues.values.zipWithIndex.map { t =>
      s""""tick.log" using 1:($$${t._2 + 2}) with fillsteps title "testbed, # of item ${t._1} to send at each second" lt 1 lc ${t._2 + 3}"""
    }
    builder.append(tickLines.mkString(""", \
"""))

    builder.append("""
unset multiplot
""")

    builder.toString()
  }
}