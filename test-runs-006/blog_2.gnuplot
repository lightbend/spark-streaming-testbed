set datafile missing "?"

set style fill solid border -1
set style line 1 lt 1
set style line 2 lt 1
set key horizontal

set terminal pngcairo dashed enhanced font "arial,10" fontscale 1.0 size 1500,999

set output "graph_2.png"

set multiplot

set lmargin 7
set rmargin 7
set bmargin 0.1
set xrange [-10:350]

set noy2tics

unset xlabel
set y2range [200: 480]
set xtics format " "
set ytics format "%.0f" nomirror 5
set yrange [0:35000]
set boxwidth 3.5

set size 1,0.3
set origin 0,0.7

set yrange [0:10]

set title "test - execution time spike, with PID rate estimator and rate limiter" offset 0,-0.5

plot "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_pid-2/pid.log" using (($1-$3-$4)/1000):(($3+$4)/1000)with boxes title "scheduling delay", \
  "" using (($1-$3-$4)/1000):($3/1000) with boxes lt 1 lc 2 title "processing time", \
  "" using ($1/1000):(5) with line lt 0 lc 3 title "batch interval"

set size 1,0.2
set origin 0,0.5

set bmargin 3
set yrange [0:45000]
set noytics
set xtics format "%.0f"
set boxwidth 1
unset title

plot "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_pid-2/tick-for-blog.log" using ($1/1000):($2 + 2 * $3) with steps lc 3 title "estimated processing time for each batch", \
  "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_pid-2/droppedValuesPerSecond_0.log" using ($1/1000):($2 * 2) with boxes title "testbed, # of item dropped per second" lt 1 lc 1

set size 1,0.3
set origin 0,0.2

set bmargin 0.1
set xtics format " "
set yrange [0:10]
set ytics format "%.0f" nomirror 5
set boxwidth 3.5

set title "test - execution time spike, with PID rate estimator and Reactive Stream back pressure"

plot "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_RS-2/pid.log" using (($1-$3-$4)/1000):(($3+$4)/1000)with boxes title "scheduling delay", \
  "" using (($1-$3-$4)/1000):($3/1000) with boxes lt 1 lc 2 title "processing time", \
  "" using ($1/1000):(5) with line lt 0 lc 3 title "batch interval"

set size 1,0.2
set origin 0,0

set bmargin 3
set yrange [0:56500]
set xlabel "timeline (in seconds)"
set xtics format "%.0f"
set noytics
set boxwidth 1
unset title

plot "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_RS-2/tick-for-blog.log" using ($1/1000):($2 + 2 * $3) with steps lc 3 title "estimated processing time for each batch", \
  "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_RS-2/droppedValuesPerSecond_0.log" using ($1/1000):($2 * 2) with boxes title "testbed, # of item dropped per second" lt 1 lc 1

unset multiplot

