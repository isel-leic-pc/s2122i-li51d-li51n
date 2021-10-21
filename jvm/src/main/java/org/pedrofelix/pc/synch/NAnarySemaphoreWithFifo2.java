package org.pedrofelix.pc.synch;

import org.pedrofelix.pc.utils.NodeLinkedList;
import org.pedrofelix.pc.utils.Timeouts;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Semaphore with n-ary acquisition and release, and FIFO order guarantee.
 * Uses specific notification by having a {@link Condition} per request.
 */
public class NAnarySemaphoreWithFifo2 {

    private static class Request {
        public final int requestedUnits;
        public final Condition condition;

        public Request(int requestedUnits, Lock monitor) {

            this.requestedUnits = requestedUnits;
            this.condition = monitor.newCondition();
        }
    }

    private final Lock monitor = new ReentrantLock();
    private final NodeLinkedList<Request> queue = new NodeLinkedList<>();

    private int units;

    public NAnarySemaphoreWithFifo2(int initialUnits) {
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
            if (queue.isEmpty() && units >= requestedUnits) {
                units -= requestedUnits;
                return true;
            }

            if (Timeouts.noWait(timeoutInMs)) {
                return false;
            }

            // wait-path
            long deadline = Timeouts.deadlineFor(timeoutInMs);
            long remaining = Timeouts.remainingUntil(deadline);
            NodeLinkedList.Node<Request> myNode = queue.enqueue(new Request(requestedUnits, monitor));
            while (true) {
                try {
                    myNode.value.condition.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    queue.remove(myNode);
                    signalIfNeeded();
                    throw e;
                }

                // is the condition true?
                if (queue.isHeadNode(myNode) && units >= requestedUnits) {
                    units -= requestedUnits;
                    queue.remove(myNode);
                    signalIfNeeded();
                    return true;
                }

                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    queue.remove(myNode);
                    signalIfNeeded();
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
            signalIfNeeded();
        } finally {
            monitor.unlock();
        }
    }

    private void signalIfNeeded() {
        if (queue.isNotEmpty() && units >= queue.getHeadValue().requestedUnits) {
            queue.getHeadValue().condition.signal();
        }
    }
}
