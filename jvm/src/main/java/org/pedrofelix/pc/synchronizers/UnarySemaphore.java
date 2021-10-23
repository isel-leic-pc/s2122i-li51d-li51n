package org.pedrofelix.pc.synchronizers;

import org.pedrofelix.pc.utils.Timeouts;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Semaphore with unary acquisition and release.
 */
public class UnarySemaphore {

    private final Lock monitor = new ReentrantLock();
    private final Condition hasUnits = monitor.newCondition();

    private int units;

    public UnarySemaphore(int initialUnits) {
        units = initialUnits;
    }

    public boolean acquire(long timeoutInMs)
            throws InterruptedException {

        monitor.lock();
        try {

            // fast-path (non wait-path)
            if (units > 0) {
                units -= 1;
                return true;
            }

            if (Timeouts.noWait(timeoutInMs)) {
                return false;
            }

            // wait-path
            long deadline = Timeouts.deadlineFor(timeoutInMs);
            long remaining = Timeouts.remainingUntil(deadline);
            while (true) {
                try {
                    hasUnits.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // Not really needed on Java,
                    // see https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html#jls-17.2.4
                    if (units > 0) {
                        hasUnits.signal();
                    }
                    throw e;
                }

                // is the required condition true?
                if (units > 0) {
                    units -= 1;
                    return true;
                }

                // recompute remaining and check if deadline already reached
                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    return false;
                }
            }
        } finally {
            monitor.unlock();
        }
    }

    public void release() {
        monitor.lock();
        try {
            units += 1;
            hasUnits.signal();
        } finally {
            monitor.unlock();
        }
    }
}
