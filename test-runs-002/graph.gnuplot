set y2range [0:]
set y2tics
set yrange [0:]
set boxwidth 1500 absolute
set style arrow 1 nohead ls 1

set xlabel "timeline (in milliseconds)"
set ylabel "execution time (in milliseconds)"
set y2label "# of items"

set terminal png size 2000,1000
set output "graph.png"

plot "execution-processed.log" using 2:(0):($1-$2):3 with vector title "delay + processing, of each batch" arrowstyle 1, "" using 2:4 axes x1y2 with line title "# of items processed per batch", "memory-processed.log" using 1:($3/10) with lines title "free memory to store the blocks", "feedback-processed.log" using 1:($3 * 25) axes x1y2 with lines title "feedback bound, # of item per batch"

