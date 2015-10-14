package com.github.olivereivak.p2md5.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.olivereivak.p2md5.model.HttpRequest;

public class SimpleHttpServer implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(SimpleHttpServer.class);

    private BlockingQueue<HttpRequest> requestQueue;

    private int port;

    public SimpleHttpServer(int port, BlockingQueue<HttpRequest> requestQueue) {
        this.port = port;
        this.requestQueue = requestQueue;
    }

    @Override
    public void run() {

        ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                try {
                    HttpRequestHandler request = new HttpRequestHandler(socket, requestQueue);

                    Thread requestHandler = new Thread(request);
                    requestHandler.setName("http-request-handler");
                    requestHandler.setDaemon(true);
                    requestHandler.start();
                } catch (Exception e) {
                    logger.error("Error creating new HttpRequestHandler.", e);
                }
            }
        } catch (IOException e) {
            logger.error("Server error. ", e);
        }

    }

    public int getPort() {
        return port;
    }

}
