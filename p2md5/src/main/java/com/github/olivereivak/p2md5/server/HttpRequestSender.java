package com.github.olivereivak.p2md5.server;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.olivereivak.p2md5.model.HttpRequest;

public class HttpRequestSender implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(HttpRequestSender.class);

    private BlockingQueue<HttpRequest> outgoingQueue;

    private int outputPort;

    public HttpRequestSender(BlockingQueue<HttpRequest> outgoingQueue) {
        this.outgoingQueue = outgoingQueue;
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            try {
                sendRequest(outgoingQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    private void sendRequest(HttpRequest request) throws InterruptedException {
        logger.debug("Sending request {} {}", request.getMethod(), request.getUri());

        try {
            Socket socket = new Socket(request.getIp(), request.getPort());
            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes());

            output.flush();
            output.close();
            socket.close();
        } catch (Exception e) {
            logger.error("Failed to create socket on port " + outputPort, e);
        }

    }

    private String getIp() {
        try {
            return InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            logger.error("Failed to get ip. ", e);
            return "";
        }
    }

    public void setOutputPort(int outputPort) {
        this.outputPort = outputPort;
    }

}
