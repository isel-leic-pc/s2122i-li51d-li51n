package org.pedrofelix.pc.apps.echoserver;

import java.io.BufferedWriter;
import java.io.IOException;

public class Utils {

    private Utils() {
        // static class
    }

    public static final void writeLine(BufferedWriter writer, String s) throws IOException {
        writer.write(s);
        writer.newLine();
        writer.flush();
    }

    public static final void writeLine(BufferedWriter writer, String format, Object... values) throws IOException {
        writer.write(String.format(format, (Object[]) values));
        writer.newLine();
        writer.flush();
    }

    public static void threadSleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // re-arm interruption status
            Thread.currentThread().interrupt();
        }
    }

    public static void ignoringInterrupts(InterruptibleRunnable action) {
        boolean wasInterrupted = false;
        while (true) {
            try {
                action.run();
                if (wasInterrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            } catch (InterruptedException e) {
                wasInterrupted = true;
            }
        }
    }

    @FunctionalInterface
    interface InterruptibleRunnable {
        void run() throws InterruptedException;
    }

}
