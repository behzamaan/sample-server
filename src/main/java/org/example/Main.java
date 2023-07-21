package org.example;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) throws IOException {
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
        httpServer.createContext("/test",new TestHandler());
        httpServer.setExecutor(threadPoolExecutor);
        httpServer.start();

        System.out.println(" Server started on port 8001");
    }

    // Define a handler for the "/test" path.
    static class TestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Here, you can extract data from the client's request.
            // For example, you can read the request body or query parameters.

            // To read the request body:
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            System.out.println(Thread.currentThread().getName() + " Request Body: " + requestBody);

            // To read query parameters (if present):
            String query = exchange.getRequestURI().getQuery();
            System.out.println("Query Parameters: " + query);

            // Send a response back to the client (optional).
            String response = "Data received successfully!";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }
}