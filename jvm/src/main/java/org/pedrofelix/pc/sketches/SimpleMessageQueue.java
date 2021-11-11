package org.pedrofelix.pc.sketches;

import org.pedrofelix.pc.utils.NodeLinkedList;
import org.pedrofelix.pc.utils.Timeouts;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleMessageQueue<T> {

    private static class DequeueRequest<T> {
        public T message = null;
        public final Condition condition;

        public DequeueRequest(Lock lock) {
            condition = lock.newCondition();
        }
    }

    private final Lock monitor = new ReentrantLock();
    private final NodeLinkedList<T> messages = new NodeLinkedList<>();
    private final NodeLinkedList<DequeueRequest<T>> requests = new NodeLinkedList<>();

    public void enqueue(T message) {
        monitor.lock();
        try{

            if(requests.isNotEmpty()) {
                var request = requests.pull();
                request.value.message = message;
                request.value.condition.signal();
            } else {
                messages.enqueue(message);
            }

        }finally{
            monitor.unlock();
        }
    }

    public Optional<T> dequeue(long timeout) throws InterruptedException {
        monitor.lock();
        try{

            // fast-path
            if(messages.isNotEmpty()) {
                var messageNode = messages.pull();
                return Optional.of(messageNode.value);
            }

            if(Timeouts.noWait(timeout)) {
                return Optional.empty();
            }

            // wait-path
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            var myrequest = requests.enqueue(new DequeueRequest<>(monitor));
            while(true) {

                try {
                    myrequest.value.condition.await(remaining, TimeUnit.MILLISECONDS);
                }catch(InterruptedException e) {
                    if(myrequest.value.message != null) {
                        Thread.currentThread().interrupt();
                        return Optional.of(myrequest.value.message);
                    }
                    requests.remove(myrequest);
                    throw e;
                }

                if(myrequest.value.message != null) {
                    return Optional.of(myrequest.value.message);
                }

                remaining = Timeouts.remainingUntil(deadline);
                if(Timeouts.isTimeout(remaining)) {
                    // give-up
                    requests.remove(myrequest);
                    return Optional.empty();
                }

            }


        }finally{
            monitor.unlock();
        }
    }



}
