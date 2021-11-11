package org.pedrofelix.pc.sketches;

import org.pedrofelix.pc.utils.Timeouts;

public class SimpleManualResetEvent {

    private final Object monitor = new Object();
    private boolean state;

    public void set() {
        synchronized (monitor) {
            state = true;
            monitor.notifyAll();
        }
    }

    public void reset() {
        synchronized (monitor) {
            state = false;
        }
    }

    public boolean waitUntilSet(long timeout) throws InterruptedException {
        synchronized (monitor) {
            // fast-path
            if(state) {
                return true;
            }
            if(Timeouts.noWait(timeout)) {
                return false;
            }

            // wait-path
            long deadline = Timeouts.deadlineFor(timeout);
            long remaining = Timeouts.remainingUntil(deadline);
            while(true) {
                monitor.wait(remaining);
                if(state) {
                    return true;
                }
                remaining = Timeouts.remainingUntil(deadline);
                if(Timeouts.isTimeout(remaining)) {
                    return false;
                }
            }
        }
    }
}
