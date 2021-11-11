package org.pedrofelix.pc.sketches;

import org.pedrofelix.pc.utils.NodeLinkedList;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class NArySemaphoreWithAsynchronousAcquire {

    private static class Request {
        public final int requestedUnits;
        public final Condition condition;
        // TODO use an enum instead of two flags
        public boolean isDone = false;
        public boolean isCancelled = false;

        public Request(int requestedUnits, Lock monitor) {

            this.requestedUnits = requestedUnits;
            this.condition = monitor.newCondition();
        }
    }

    private final Lock monitor = new ReentrantLock();
    private final NodeLinkedList<Request> acquireRequests = new NodeLinkedList<>();

    private int units;

    public NArySemaphoreWithAsynchronousAcquire(int initialUnits) {
        units = initialUnits;
    }

    public Future<Integer> acquire(int requestedUnits) {

        monitor.lock();
        try {

            // fast-path
            if (acquireRequests.isEmpty() && units >= requestedUnits) {
                units -= requestedUnits;
                return new CompletedFuture(requestedUnits);
            }

            // wait-path
            NodeLinkedList.Node<Request> myNode = acquireRequests.enqueue(new Request(requestedUnits, monitor));

            return new NodeBasedFuture(myNode);

        } finally {
            monitor.unlock();
        }
    }

    public void release(int releasedUnits) {
        monitor.lock();
        try {
            units += releasedUnits;
            completeAllRequestsThatCanBeCompleted();
        } finally {
            monitor.unlock();
        }
    }

    private void completeAllRequestsThatCanBeCompleted() {
        while (acquireRequests.isNotEmpty() && units >= acquireRequests.getHeadValue().requestedUnits) {
            Request headRequest = acquireRequests.pull().value;
            headRequest.isDone = true;
            units -= headRequest.requestedUnits;
            // Each `get` on the associated future will result on an `await` on this condition
            // Since multiple threads can do a `get` on the same future, we need to use broadcast here (i.e. signalAll)
            headRequest.condition.signalAll();
        }
    }

    /**
     * A Future that refers to a node in the acquireRequests queue.
     * This is an inner class, so that it can access the fields of the creating object,
     * namely the monitor.
     * All methods acquire the lock of the associated semaphore object.
     */
    private class NodeBasedFuture implements Future<Integer> {

        // The node in the acquireRequests queue
        private final NodeLinkedList.Node<Request> node;

        NodeBasedFuture(NodeLinkedList.Node<Request> node) {

            this.node = node;
        }

        @Override
        public boolean isCancelled() {
            monitor.lock();
            try {
                return node.value.isCancelled;
            } finally {
                monitor.unlock();
            }
        }

        @Override
        public boolean isDone() {
            monitor.lock();
            try {
                return node.value.isDone;
            } finally {
                monitor.unlock();
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            monitor.lock();
            try {
                if (node.value.isDone) {
                    return false;
                }
                if (node.value.isCancelled) {
                    return true;
                }
                // node is in the queue
                acquireRequests.remove(node);
                node.value.isCancelled = true;
                node.value.condition.signalAll();
                return true;
            } finally {
                monitor.unlock();
            }
        }


        @Override
        public Integer get() throws InterruptedException {
            monitor.lock();
            try {
                while (true) {
                    if (node.value.isDone) {
                        return node.value.requestedUnits;
                    }
                    if (node.value.isCancelled) {
                        throw new CancellationException();
                    }
                    node.value.condition.await();
                    // no catch because there are no actions needed on withdrawal due to InterruptedException
                }
            } finally {
                monitor.unlock();
            }
        }

        @Override
        public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            // left as an exercise
            throw new UnsupportedOperationException("TODO");
        }
    }

    /**
     * A Future that is already completed when created.
     */
    private static class CompletedFuture implements Future<Integer> {

        private final int n;

        CompletedFuture(int n) {

            this.n = n;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Integer get() {
            return n;
        }

        @Override
        public Integer get(long timeout, TimeUnit unit) {
            return n;
        }
    }

}
