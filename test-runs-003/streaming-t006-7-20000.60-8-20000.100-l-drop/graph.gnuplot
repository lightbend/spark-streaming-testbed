
set y2range [0:]
set y2tics
set yrange [0:]
set lmargin 12
set rmargin 10
set datafile missing "?"
set style fill transparent solid 0.25

set style arrow 1 nohead ls 1
set ytics nomirror

set terminal pngcairo dashed enhanced font "arial,10" fontscale 1.0 size 1500,1000
set output "graph.png"

set multiplot layout 3, 1 title "Streaming back pressure branch, TCP receiver, congestion strategy: drop. spikes? Changing computation time, not volume"

set xrange [ -8069 : 680046 ]
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
set y2range [ 0 : 1.1 ]

plot "ratio.log" using 1:2 axes x1y2 with lines title "Congestion strategie - drop ratio, for each block" lt 1 lc rgb "#DDDDDD", \
  "ratio.log" u 1:2 axes x1y2 smooth bezier title "smoothed drop ratio" lt 1 lc "black", \
"execution.log" using 2:($3) with filledcurve x1 title "Spark - # of items 7 processed per batch" lt 1 lc 3, \
"execution.log" using 2:($4) with filledcurve x1 title "Spark - # of items 8 processed per batch" lt 1 lc 4, \
  "feedback.log" using 1:($2 * 25) with lines title "Spark - feedback bound, max # of item per batch" lt 1 lc 2


set xlabel "timeline (in milliseconds)"


set xtics format "%g"
set bmargin 3
set ylabel "# of items"
unset y2label
unset y2tics
unset y2range


set yrange [ 0 : 24000 ]
set y2range [ 0 : 24 ]

set boxwidth 1000

plot "droppedValuesPerSecond.log" using 1:2 with boxes title "testbed, # of item dropped per second, as TCP socket was not ready" lt 1 lc 1, \
"tick.log" using 1:($2) with fillsteps title "testbed, # of item 7 to send at each second" lt 1 lc 3, \
"tick.log" using 1:($3) with fillsteps title "testbed, # of item 8 to send at each second" lt 1 lc 4
unset multiplot
