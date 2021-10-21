package org.pedrofelix.pc.utils;

import java.util.concurrent.TimeUnit;

public final class Timeouts {

    private Timeouts() {
        // static class
    }

    public static boolean noWait(long timeout) {
        return timeout <= 0;
    }

    /**
     * Returns a {@link long} representing the deadline for a timeout.
     *
     * @param duration the timeout duration.
     * @param timeUnit the duration unit.
     * @return the deadline for the timeout.
     */
    public static long deadlineFor(long duration, TimeUnit timeUnit) {
        return deadlineFor(timeUnit.toMillis(duration));
    }

    public static long deadlineFor(long timeout) {
        return System.currentTimeMillis() + timeout;
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    /**
     * Returns the amount of milliseconds remaining for the timeout deadline.
     *
     * @param deadline the timeout deadline
     * @return the amount of milliseconds remaining for the timeout deadline.
     */
    public static long remainingUntil(long deadline) {
        return deadline - System.currentTimeMillis();
    }

    /**
     * Checks if the timeout deadline was already reached
     *
     * @param remaining the remaining time for the timeout's deadline
     * @return {@code true} if the timeout was reached, {@code false} otherwise.
     */
    public static boolean isTimeout(long remaining) {
        return remaining <= 0;
    }

}