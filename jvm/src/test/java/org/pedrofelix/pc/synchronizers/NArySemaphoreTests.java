package org.pedrofelix.pc.synchronizers;

import org.junit.Test;
import org.pedrofelix.pc.utils.TestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class NArySemaphoreTests {

    private static final int N_OF_THREADS = 10;
    private static final Duration TEST_DURATION = Duration.ofSeconds(10);

    private void does_not_exceed_max_units(NArySemaphore semaphore, int units) throws InterruptedException {
        AtomicInteger acquiredUnits = new AtomicInteger(units);
        TestHelper helper = new TestHelper(TEST_DURATION);

        helper.createAndStartMultiple(N_OF_THREADS, (ignore, isDone) -> {
            while (!isDone.get()) {
                int requestedUnits = ThreadLocalRandom.current().nextInt(units) + 1;
                assertTrue("acquire must succeed",semaphore.acquire(requestedUnits, Long.MAX_VALUE));
                try {
                    int current = acquiredUnits.addAndGet(-requestedUnits);
                    assertTrue("acquiredUnits must not be negative", current >= 0);
                    Thread.yield();
                } finally {
                    acquiredUnits.addAndGet(requestedUnits);
                    semaphore.release(requestedUnits);
                }
            }
        });

        helper.join();
    }

    @Test
    public void NArySemaphoreWithoutOrder_simple_test() throws InterruptedException {
        int units = N_OF_THREADS / 3;
        does_not_exceed_max_units(new NArySemaphoreWithoutOrder(units), units);
    }

    @Test
    public void NArySemaphoreWithFifo_simple_test() throws InterruptedException {
        int units = N_OF_THREADS / 3;
        does_not_exceed_max_units(new NArySemaphoreWithFifo(units), units);
    }

    @Test
    public void NArySemaphoreWithFifo2_simple_test() throws InterruptedException {
        int units = N_OF_THREADS / 3;
        does_not_exceed_max_units(new NArySemaphoreWithFifo2(units), units);
    }

    @Test
    public void NArySemaphoreWithFifo3_simple_test() throws InterruptedException {
        int units = N_OF_THREADS / 3;
        does_not_exceed_max_units(new NArySemaphoreWithFifo3(units), units);
    }

}
