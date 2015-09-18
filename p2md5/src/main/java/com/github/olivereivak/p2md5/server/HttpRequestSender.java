package com.github.olivereivak.p2md5.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

import com.github.olivereivak.p2md5.model.HttpRequest;

public class HttpRequestSender implements Runnable {

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
        System.out.println("HttpRequestSender: " + request.getMethod() + " " + request.getUri());

    }

    private String getIp() {
        try {
            return InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            System.out.println(e);
            return "";
        }
    }

    public void setOutputPort(int outputPort) {
        this.outputPort = outputPort;
    }

}
