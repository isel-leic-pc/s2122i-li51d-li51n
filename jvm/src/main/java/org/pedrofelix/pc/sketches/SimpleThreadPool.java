package org.pedrofelix.pc.sketches;

import org.pedrofelix.pc.utils.NodeLinkedList;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class SimpleThreadPool {

    private final int maxWorkers;
    private final Object monitor = new Object();
    private NodeLinkedList<Runnable> workItems = new NodeLinkedList<>();
    private int currentWorkers = 0;

    public SimpleThreadPool(int maxWorkers) {
        this.maxWorkers = maxWorkers;
    }

    public void execute(Runnable work) {
        synchronized (monitor) {
            if (currentWorkers < maxWorkers) {
                var th = new Thread(() -> threadLoop(work));
                th.start();
                currentWorkers += 1;
            } else {
                workItems.enqueue(work);
            }
        }
    }
    private Optional<Runnable> getWork() {
        synchronized (monitor) {
            if (workItems.isNotEmpty()) {
                return Optional.of(workItems.pull().value);
            } else {
                currentWorkers -= 1;
                return Optional.empty();
            }
        }
    }

    private void threadLoop(Runnable firstWork) {
        firstWork.run();
        while (true) {
            var maybeWork = getWork();
            maybeWork.ifPresent(work -> work.run());
            if (maybeWork.isEmpty()) {
                return;
            }
        }
    }
}
