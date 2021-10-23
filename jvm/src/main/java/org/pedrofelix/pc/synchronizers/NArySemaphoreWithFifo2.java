package org.pedrofelix.pc.synchronizers;

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
public class NArySemaphoreWithFifo2 {

    private static class Request {
        public final int requestedUnits;
        public final Condition condition;

        public Request(int requestedUnits, Lock monitor) {

            this.requestedUnits = requestedUnits;
            this.condition = monitor.newCondition();
        }
    }

    private final Lock monitor = new ReentrantLock();
    private final NodeLinkedList<Request> requests = new NodeLinkedList<>();

    private int units;

    public NArySemaphoreWithFifo2(int initialUnits) {
        units = initialUnits;
    }

    public boolean acquire(int requestedUnits, long timeoutInMs)
            throws InterruptedException {

        monitor.lock();
        try {

            // fast-path (non wait-path)
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
            NodeLinkedList.Node<Request> myRequestNode =
                    requests.enqueue(new Request(requestedUnits, monitor));
            while (true) {
                try {
                    myRequestNode.value.condition.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    requests.remove(myRequestNode);
                    signalIfNeeded();
                    throw e;
                }

                // is the condition true?
                if (requests.isHeadNode(myRequestNode) && units >= requestedUnits) {
                    units -= requestedUnits;
                    requests.remove(myRequestNode);
                    signalIfNeeded();
                    return true;
                }

                remaining = Timeouts.remainingUntil(deadline);
                if (Timeouts.isTimeout(remaining)) {
                    requests.remove(myRequestNode);
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
        if (requests.isNotEmpty() &&
                units >= requests.getHeadValue().requestedUnits) {
            requests.getHeadValue().condition.signal();
        }
    }
}
