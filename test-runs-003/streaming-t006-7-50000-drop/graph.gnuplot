
set y2range [0:]
set y2tics
set yrange [0:]
set lmargin 12
set rmargin 10

set style arrow 1 nohead ls 1
set ytics nomirror

set terminal pngcairo dashed enhanced font "arial,10" fontscale 1.0 size 1500,1000
set output "graph.png"

set multiplot layout 3, 1 title "Streaming back pressure branch, TCP receiver, congestion strategy: drop"

set xrange [ -9363 : 340041 ]
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

plot "ratio.log" using 1:2 axes x1y2 with lines title "Congestion strategie - drop ratio, for each block" lt 1 lc rgb "#DDDDDD", \
  "ratio.log" u 1:2 axes x1y2 smooth bezier title "smoothed drop ratio" lt 1 lc "black", \
"execution.log" using 2:4 with line title "Spark - # of items processed per batch" lt 1 lc 3, \
  "feedback.log" using 1:($2 * 25) with lines title "Spark - feedback bound, max # of item per batch" lt 1 lc 4


set xlabel "timeline (in milliseconds)"


set xtics format "%g"
set bmargin 3
set ylabel "# of items"
unset y2label
unset y2tics


set yrange [ 0 : 60000 ]
set y2range [ 0 : 600 ]

set boxwidth 1000

plot "tick.log" using 1:2 with steps title "testbed, # of item to send at each second" lt 1 lc 1, \
  "droppedValuesPerSecond.log" using 1:2 with boxes title "testbed, # of item dropped per second, as TCP socket was not ready" lt 1 lc 2


unset multiplot
