package org.pedrofelix.pc.apps.echoserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class EchoServer_3_MultiThreaded_Bounded_Semaphore {

    private static final Logger logger = LoggerFactory.getLogger(EchoServer_3_MultiThreaded_Bounded_Semaphore.class);
    private static final int PORT = 8080;
    private static final String ADDRESS = "0.0.0.0";
    private static final String EXIT_LINE = "exit";

    public static void main(String[] args) throws IOException {
        new EchoServer_3_MultiThreaded_Bounded_Semaphore().run();
    }

    private void run() throws IOException {
        var serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(ADDRESS, PORT));
        logger.info("server socket bound to {}:{}", ADDRESS, PORT);
        acceptLoop(serverSocket);
    }

    private void acceptLoop(ServerSocket serverSocket) throws IOException {
        while (true) {
            acquirePermissionToRunEchoLoop();
            var socket = serverSocket.accept();
            logger.info("client socket accepted, remote address is {}", socket.getInetAddress().getHostAddress());
            new Thread(() -> echoLoop(socket)).start();
        }
    }

    private void echoLoop(Socket socket) {
        int clientNo = getNewClientNumber();
        int lineNo = 0;
        try (socket;
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             var writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            Utils.writeLine(writer, "Hi! You are client number %s", Integer.toString(clientNo));
            while (true) {

                var line = reader.readLine();
                if (line == null || line.equals(EXIT_LINE)) {
                    Utils.writeLine(writer, "Bye.");
                    socket.close();
                    return;
                }
                logger.info("Received line '{}', echoing it back", line);
                Utils.writeLine(writer, "%d: %s", lineNo++, line.toUpperCase());
            }
        } catch (IOException e) {
            logger.warn("Connection ended with IO error: {}", e.getMessage());
        } finally {
            decrementRunningEchoLoops();
        }
    }

    private final AtomicInteger clientNoCounter = new AtomicInteger(1);

    private int getNewClientNumber() {
        return clientNoCounter.getAndAdd(1);
    }

    private final Semaphore availableEchoLoops = new Semaphore(2);

    private void acquirePermissionToRunEchoLoop() {
        Utils.ignoringInterrupts(availableEchoLoops::acquire);
    }
    private void decrementRunningEchoLoops() {
        availableEchoLoops.release();
    }

}
