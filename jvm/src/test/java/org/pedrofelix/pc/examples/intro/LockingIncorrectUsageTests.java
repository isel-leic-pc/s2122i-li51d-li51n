package org.pedrofelix.pc.examples.intro;

import org.junit.Test;
import org.pedrofelix.pc.utils.TestUtils;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class LockingIncorrectUsageTests {

    private static final int N_OF_REPS = 5_000_000;
    private static final int N_OF_THREADS = 10;

    private int sharedCounter = 0;

    private final Lock theLock = new ReentrantLock();

    @Test
    public void error_mixing_intrinsic_and_explicit_locks() {

        var th1 = new Thread(() -> {
            // *intrinsic* lock is acquired
            synchronized (theLock) {
                for (int i = 0; i < N_OF_REPS; ++i) {
                    sharedCounter += 1;
                }
            }
        });
        th1.start();

        var th2 = new Thread(() -> {
            // *explicit* lock is acquired, which is not the same
            theLock.lock();
            try {
                for (int i = 0; i < N_OF_REPS; ++i) {
                    sharedCounter += 1;
                }
            } finally {
                theLock.unlock();
            }
        });
        th2.start();

        TestUtils.uninterruptibleJoin(th1);
        TestUtils.uninterruptibleJoin(th2);

        // Not that the assertion checks the values are NOT equal
        // This happens because there is NOT mutual exclusion between th1 and th2
        assertNotEquals(2 * N_OF_REPS, sharedCounter);
    }

    @Test
    public void error_using_different_lock_objects_to_protect_the_same_shared_data() {

        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS; ++i) {
            var th = new Thread(() -> {
                // There will be a different lock object PER thread, so the locking protocol is not correct.
                var theLock = new Object();
                synchronized (theLock) {
                    for (int j = 0; j < N_OF_REPS; ++j) {
                        ++sharedCounter;
                    }
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);

        // notice that the assertion is NOT equals
        assertNotEquals(N_OF_THREADS * N_OF_REPS, sharedCounter);
    }

    @Test
    public void correct_using_the_same_lock_object_to_protect_the_same_shared_data() {
        // The same lock object for all the threads
        var theLock = new Object();
        var ths = new ArrayList<Thread>(N_OF_THREADS);
        for (int i = 0; i < N_OF_THREADS; ++i) {
            var th = new Thread(() -> {
                synchronized (theLock) {
                    for (int j = 0; j < N_OF_REPS; ++j) {
                        ++sharedCounter;
                    }
                }
            });
            th.start();
            ths.add(th);
        }

        ths.forEach(TestUtils::uninterruptibleJoin);

        // notice that the assertion is IS equals
        assertEquals(N_OF_THREADS * N_OF_REPS, sharedCounter);
    }
}
