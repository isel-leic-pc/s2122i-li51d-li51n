package org.pedrofelix.pc.apps.echoserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer_0_SingleThreaded {

    private static final Logger logger = LoggerFactory.getLogger(EchoServer_0_SingleThreaded.class);
    private static final int PORT = 8080;
    private static final String ADDRESS = "0.0.0.0";
    private static final String EXIT_LINE = "exit";

    public static void main(String[] args) throws IOException {
        new EchoServer_0_SingleThreaded().run();
    }

    private void run() throws IOException {
        var serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(ADDRESS, PORT));
        logger.info("server socket bound to {}:{}", ADDRESS, PORT);
        acceptLoop(serverSocket);
    }

    private void acceptLoop(ServerSocket serverSocket) throws IOException {
        while (true) {
            var socket = serverSocket.accept();
            logger.info("client socket accepted, remote address is {}", socket.getInetAddress().getHostAddress());
            echoLoop(socket);
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
        }
    }

    private int clientNoCounter = 1;
    private int getNewClientNumber() {
        int clientNo = clientNoCounter;
        clientNoCounter += 1;
        return clientNo;
    }

}
