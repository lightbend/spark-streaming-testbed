
set y2range [0:]
set y2tics
set yrange [0:]
set lmargin 12
set rmargin 10
set datafile missing "?"
set style fill transparent solid 0.25

set style arrow 1 nohead ls 1
set style arrow 2 nohead ls 1 lc 3
set ytics nomirror

set terminal pngcairo dashed enhanced font "arial,10" fontscale 1.0 size 1500,999

set output "graph.png"

set multiplot layout 3, 1 title "1.5.0-rc3, TCP receiver, rate estimator. Execution time spike"

set xrange [ -7114 : 346590 ]
set xtics format " "
set bmargin 1
set ylabel "execution time (in milliseconds)"
set y2label "memory (in MB)"

plot "memory.log" using 1:(5000) with line lt 0 lc 3 title "batch interval", \
  "memory.log" using 1:($2/1024) axes x1y2 with lines title "Spark - free memory to store the blocks" lt 1 lc 2, \
  "pid.log" using ($1-$3-$4):(0):3:3 with vector title "processing time" arrowstyle 2, \
  "pid.log" using ($1-$4):3:4:4 with vector title "scheduling delay" arrowstyle 1


set tmargin 0
set ylabel "# of items"
set y2label "drop ratio"
set y2range [ 0 : 1.1 ]


plot "execution_0.log" using 2:($3) with filledcurve x1 title "Spark - # of items 7 processed per batch" lt 1 lc 3, \
"execution_0.log" using 2:($4) with filledcurve x1 title "Spark - # of items 8 processed per batch" lt 1 lc 4, \
  "feedback_0.log" using 1:($2 * 5) with lines title "Spark - feedback bound, max # of item per batch" lt 1 lc 2


set xlabel "timeline (in milliseconds)"


set xtics format "%.0f"
set bmargin 3
set ylabel "# of items"
unset y2label
unset y2tics
unset y2range


set yrange [ 0 : 24000 ]

set boxwidth 1000

plot "droppedValuesPerSecond_0.log" using 1:2 with boxes title "testbed, # of item dropped per second" lt 1 lc 1, \
"tick.log" using 1:($2) with fillsteps title "testbed, # of item 7 to send at each second" lt 1 lc 3, \
"tick.log" using 1:($3) with fillsteps title "testbed, # of item 8 to send at each second" lt 1 lc 4
unset multiplot
