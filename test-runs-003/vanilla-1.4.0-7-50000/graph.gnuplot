
set y2range [0:]
set y2tics
set yrange [0:]
set lmargin 12
set rmargin 10

set style arrow 1 nohead ls 1
set ytics nomirror

set terminal pngcairo dashed enhanced font "arial,10" fontscale 1.0 size 1500,1000
set output "graph.png"

set multiplot layout 3, 1 title "vanilla Spark 1.4.0, TCP receiver, no congestion strategy"

set xrange [ -11543 : 333193 ]
set xtics format " "
set bmargin 1
set ylabel "execution time (in milliseconds)"
set y2label "memory (in MB)"

plot "memory.log" using 1:(5000) with line lt 0 lc 3 title "batch interval", \
  "execution.log" using 2:(0):($1-$2):($1-$2) with vector title "Spark - delay + processing, of each batch" arrowstyle 1, \
  "memory.log" using 1:($2/1024) axes x1y2 with lines title "Spark - free memory to store the blocks" lt 1 lc 2
 


set tmargin 0
set ylabel "# of items"
set y2label "drop ratio"

plot "execution.log" using 2:4 with line title "Spark - # of items processed per batch" lt 1 lc 3


set xlabel "timeline (in milliseconds)"


set xtics format "%g"
set bmargin 3
set ylabel "# of items"
unset y2label
unset y2tics


set yrange [ 0 : 60000 ]
set y2range [ 0 : 60000 ]

set boxwidth 1000

plot "tick.log" using 1:2 with steps title "testbed, # of item to send at each second" lt 1 lc 1, \
  "droppedValuesPerSecond.log" using 1:2 with boxes title "testbed, # of item dropped per second, as TCP socket was not ready" lt 1 lc 2


unset multiplot
