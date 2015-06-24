# Initial push, need clean up work, a lot

## simple-streaming-app

Test Spark streaming app, to test the testbed.

No, there is no test for the app used to test the test app ....

It tries to connect to a listening socket on localhost:2222. It expects a stream of numbers, separated by new lines. It counts the number of numbers, and their sum. Count and sum are printed on the console.

The batch size is set to 5 seconds

### Running

Launch the main class using sbt, from the project folder.

```bash
sbt run
```

## testbed

The test bed. Execute the test plan, i.e.: push numbers on sockets according to a Typesafe config file.

It listen to port 2222, and accept multiple concurrent connection. Same data is send on all connection.

The test plan can be given through a rudimentary web interface on http://localhost:9000/ , or as file on the command line.

The command line tool terminates after the test plan has been executed.
With the web interface, it is possible to run multiple test plan consecutively. The new plan interrups the previous one, if it was still running.

The test plan look like that:
```
sequence = [
  { type = noop         # does nothing, for the duration
    duration = 2        # allow to temporize, to be sure that the client have connected to the socket.
  }
  { type = ramp         # ramp up or down
    startRate = 1000    # the rate for the first second is startRate, the rate for the last second is endRate
    endRate = 50000     # with a linear progression for the seconds in between
    value = 5
    duration = 10       # duration is not optional for this phase type
  }
  { type = fixed        # push the same number (value), for the duration, at the given rate.
    value = 1           # rate: number of number pushed per second
    rate = 4
    duration = 1
  }
  { type = cycle        # cycle through 'values'
    values = [5, 5, 5, 7, 5, 5, 5]
    rate = 4
    duration = 1
  }
]
```

### Running

To run the webapp, start play from teh project folder.

```bash
sbt run
```

To run the command line app, use sbt runMain from the project folder.

```bash
sbt 'runMain com.typesafe.spark.testbed.TestBed /path/to/testplan'
```
