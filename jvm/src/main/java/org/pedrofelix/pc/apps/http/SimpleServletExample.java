package org.pedrofelix.pc.apps.http;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/******************************************************************************
 *
 * Example using the Java servlet API to illustrate:
 *
 * - How different requests are handled by (potentially) distinct threads,
 *   even if the application never creates a thread.
 *
 * - How the same servlet instance is shared between multiple requests and multiple threads.
 *
 * - The consequences of using local variables vs. instance fields for mutable data
 *
 * Use to perform concurrent requests
 *
 *  curl http://localhost:8080/path1 & curl http://localhost:8080/path2
 *
 */
public class SimpleServletExample {

    private static final Logger log = LoggerFactory.getLogger(SimpleServletExample.class);
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        Server server = new Server(PORT);
        ServletHandler handler = new ServletHandler();

        // Note on how we are creating a *single* servlet, which will be shared between all threads
        TheServlet servlet = new TheServlet();

        handler.addServletWithMapping(new ServletHolder(servlet), "/*");
        log.info("registered {} on all paths", servlet);

        server.setHandler(handler);
        server.start();
        log.info("server started listening on port {}", PORT);

        log.info("Waiting for server to end");
        server.join();

        log.info("main is ending");
    }

    static class TheServlet extends HttpServlet {

        private String fieldRequestURI;

        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            log.info("doGet request: URI='{}', method='{}", request.getMethod(), request.getRequestURI());

            // private to the thread
            String localRequestURI = request.getRequestURI();

            // SHARED between all threads
            this.fieldRequestURI = request.getRequestURI(); // T1: 123 T2: abc

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.warn("Sleep interrupted, continuing");
            }

            String bodyString = String.format("Request processed on thread '%s', method='%s', URI='%s', URI = '%s'\n",
                    Thread.currentThread().getName(),
                    request.getMethod(),
                    localRequestURI,
                    fieldRequestURI
            );
            byte[] bodyBytes = bodyString.getBytes(StandardCharsets.UTF_8);

            response.addHeader("Content-Type", "text/plain, charset=utf-8");
            response.addHeader("Content-Length", Integer.toString(bodyBytes.length));
            response.getOutputStream().write(bodyBytes);
        }
    }
}