set datafile missing "?"

set boxwidth 3.5
set style fill solid border -1
set style line 1 lt 1
set style line 2 lt 1
set key horizontal

set terminal pngcairo dashed enhanced font "arial,10" fontscale 1.0 size 1500,999

set output "graph_1.png"

set multiplot

set lmargin 7
set rmargin 7
set xrange [-10:350]

set size 1,0.6
set origin 0,0.4

set bmargin 0.1
unset xlabel
set y2range [200: 480]
set xtics format " "
set ytics format "%.0f" nomirror 5
set y2tics format "%.0f" 50
set yrange [0:35]

set title "test - execution time spike, with no rate estimator or rate limiter" offset 0,-0.5

plot "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_nopid/pid.log" using (($1-$3-$4)/1000):(($3+$4) / 1000)with boxes title "sheduling delay (seconds)", \
  "" using (($1-$3-$4)/1000):($3/1000) with boxes lt 1 lc 2 title "processing time (seconds)", \
  "" using ($1/1000):(5) with line lt 0 lc 3 title "batch interval (seconds)", \
  "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_nopid/memory.log" using ($1/1000):($2/1024) axes x1y2 with lines title "free memory (MB)" lt 1 lc 3

set size 1,0.20
set origin 0,0.2

set yrange [0:10]
set y2range [200: 280]

set title "test - execution time spike, with PID rate estimator and rate limiter"

plot "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_pid-2/pid.log" using (($1-$3-$4)/1000):(($3+$4)/1000)with boxes title "scheduling delay", \
  "" using (($1-$3-$4)/1000):($3/1000) with boxes lt 1 lc 2 title "processing time", \
  "" using ($1/1000):(5) with line lt 0 lc 3 title "batch interval", \
  "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_pid-2/memory.log" using ($1/1000):($2/1024) axes x1y2 with lines title "free memory" lt 1 lc 3

set size 1,0.2
set origin 0,0

set bmargin 3
set yrange [0:45000]
set xlabel "timeline (in seconds)"
set xtics format "%.0f"
set noytics
set noy2tics
unset title

plot "1.5.0-rc3-7-25000.60-8-25000.100-7-25000.150_pid-2/tick-for-blog.log" using ($1/1000):($2 + 2 * $3) with steps lc 3 title "estimated processing time for each batch"

unset multiplot
