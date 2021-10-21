package org.pedrofelix.pc.synch;

import org.pedrofelix.pc.utils.Timeouts;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Semaphore with n-ary acquisition and release, and no acquisition order guarantee.
 */
public class NAnarySemaphore {

    private final Lock monitor = new ReentrantLock();
    private final Condition hasUnits = monitor.newCondition();
    private int units;

    public NAnarySemaphore(int initialUnits) {
        units = initialUnits;
    }

    public boolean acquire(int requestedUnits, long timeoutInMs)
            throws InterruptedException {

        monitor.lock();
        try {

            if (timeoutInMs < 0) {
                throw new IllegalArgumentException("timeoutInMs must be >=0");
            }

            // fast-path (non wait-path)
            if (units >= requestedUnits) {
                units -= requestedUnits;
                return true;
            }

            if (Timeouts.noWait(timeoutInMs)) {
                return false;
            }

            // wait-path
            long deadline = Timeouts.deadlineFor(timeoutInMs);
            long remaining = Timeouts.remainingUntil(deadline);
            while (true) {

                // No need to handle exceptions because there are not lost notifications
                hasUnits.await(remaining, TimeUnit.MILLISECONDS);

                if (units >= requestedUnits) {
                    units -= requestedUnits;
                    return true;
                }

                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    return false;
                }
            }
        } finally {
            monitor.unlock();
        }
    }

    public void release(int releasedUnits) {
        monitor.lock();
        try {
            units += releasedUnits;
            // notice that a signalAll is required to ensure the correct thread leaves the wait-set
            hasUnits.signalAll();
        } finally {
            monitor.unlock();
        }
    }
}
