package org.pedrofelix.pc.synchronizers;

import org.pedrofelix.pc.utils.NodeLinkedList;
import org.pedrofelix.pc.utils.Timeouts;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Semaphore with n-ary acquisition and release, and FIFO order guarantee.
 */
public class NArySemaphoreWithFifo {

    private static class Request {
        public final int requestedUnits;

        public Request(int requestedUnits) {
            this.requestedUnits = requestedUnits;
        }
    }

    private final Lock monitor = new ReentrantLock();
    private final Condition hasUnits = monitor.newCondition();

    private final NodeLinkedList<Request> requests = new NodeLinkedList<>();
    private int units;

    public NArySemaphoreWithFifo(int initialUnits) {
        units = initialUnits;
    }

    public boolean acquire(int requestedUnits, long timeoutInMs)
            throws InterruptedException {

        monitor.lock();
        try {

            // fast-path
            if (requests.isEmpty() && units >= requestedUnits) {
                units -= requestedUnits;
                return true;
            }

            if (Timeouts.noWait(timeoutInMs)) {
                return false;
            }

            // wait-path
            long deadline = Timeouts.deadlineFor(timeoutInMs);
            long remaining = Timeouts.remainingUntil(deadline);
            NodeLinkedList.Node<Request> myNode =
                    requests.enqueue(new Request(requestedUnits));
            while (true) {
                try {
                    hasUnits.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    requests.remove(myNode);
                    signalAllIfNeeded();
                    throw e;
                }

                // is the condition true?
                if (requests.isHeadNode(myNode) && units >= requestedUnits) {
                    units -= requestedUnits;
                    requests.remove(myNode);
                    signalAllIfNeeded();
                    return true;
                }

                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    requests.remove(myNode);
                    signalAllIfNeeded();
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
            signalAllIfNeeded();
        } finally {
            monitor.unlock();
        }
    }

    private void signalAllIfNeeded() {
        if (requests.isNotEmpty() &&
                units >= requests.getHeadValue().requestedUnits) {
            hasUnits.signalAll();
        }
    }
}
