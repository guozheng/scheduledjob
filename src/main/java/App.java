import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by gzge. All Rights Reserved
 */
public class App {

    static class GoodWorker implements Runnable {

        @Override
        public void run() {
            System.out.println("GoodWorker start working");
        }
    }

    static class BadWorker implements Runnable {

        @Override
        public void run() {
            System.out.println("BadWorker start working");
            oops();
        }
    }

    static class BadWorkerFixed implements Runnable {

        @Override
        public void run() {
            System.out.println("BadWorkerFixed start working");
            try {
                oops();
            } catch (Exception ex) {
                System.out.println("BadWorkerFixed got exception: " + ex.getMessage());
            }
        }
    }

    public static void oops() throws NullPointerException {
        throw new NullPointerException("Oops...");
    }

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        scheduler.scheduleWithFixedDelay(new GoodWorker(), 0, 2, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(new BadWorker(), 0, 2, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(new BadWorkerFixed(), 0, 2, TimeUnit.SECONDS);
    }
}
