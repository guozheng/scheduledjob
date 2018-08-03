# scheduledjob
A small demo app to show how a scheduled job could die silently if it throws an `Exception` not being caught.

The app is dead simple, it uses `ScheduledExecutorService` to run three tasks with fixed delay:

   1. `GoodWorker`: a normal worker thread that prints a message
   2. `BadWorker`: a bad worker thread prints a message then throws an uncaught NPE
   3. `BadWorker`: a bad worker similar to #2, but have a try-catch block for `Exception`

The outcome will be that `BadWorker` only runs for the first time, then it silently stops execution for the next round. Adding try-catch block fixes the issue.

The root cause lies in `runAndReset` method of `FutureTask`. When a `Runnable` is submitted to `ScheduledExecutorService`, it is wrapped in a `ScheduledFutureTask` with time to trigger the action. Then, the task is put to a task queue to wait for the time to come for execution. `ScheduledFutureTask.run` method is invoked when time comes to execute the task, for periodic task (our case), it calls its super class method `FutureTask.runAndReset`:

```java
    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex);
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }
``` 

Notice when `c.call` method throws a `Throwable`, it sets exception but does not reset task to initial state, thus the task will not be invoked again. So, what we observe is that the task silently stops execution.

## Running the app
```bash
> ./gradlew run

> Task :run
GoodWorker start working
BadWorker start working
BadWorkerFixed start working
BadWorkerFixed got exception: Oops...
GoodWorker start working
BadWorkerFixed start working
BadWorkerFixed got exception: Oops...
GoodWorker start working
BadWorkerFixed start working
BadWorkerFixed got exception: Oops...
GoodWorker start working
BadWorkerFixed start working
BadWorkerFixed got exception: Oops...
<=========----> 75% EXECUTING [8s]

```