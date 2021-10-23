package org.pedrofelix.pc.synchronizers;

public interface NArySemaphore {

    boolean acquire(int requestedUnits, long timeoutInMs) throws InterruptedException;

    void release(int releasedUnits);
}
