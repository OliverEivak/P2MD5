package com.github.olivereivak.p2md5.server;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.Peer;

public class HttpRequestSender implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(HttpRequestSender.class);

    private HttpRequest httpRequest;

    private int outputPort;

    private List<Peer> peers;

    public HttpRequestSender(HttpRequest httpRequest, List<Peer> peers) {
        this.httpRequest = httpRequest;
        this.peers = peers;
    }

    @Override
    public void run() {
        try {
            sendRequest(httpRequest);
        } catch (InterruptedException e) {
            logger.error("Interrupted while sending HttpRequest.");
        }
    }

    private void sendRequest(HttpRequest request) throws InterruptedException {
        logger.debug("Trying to send request to {} {} {}", request.getIp() + ":" + request.getPort(),
                request.getMethod(), request.getPath());

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(request.getIp(), request.getPort()), 1000);

            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes());

            output.flush();
            output.close();
            socket.close();
        } catch (Exception e) {
            logger.error("Failed to create socket to " + request.getIp().getHostAddress() + ":" + request.getPort());

            // Mark peer as unreachable
            synchronized (peers) {
                for (Peer peer : peers) {
                    String requestIp = request.getIp().getHostAddress();
                    int requestPort = request.getPort();
                    if (peer.getIp().equals(requestIp) && peer.getPort() == requestPort) {
                        peer.setReachable(false);
                    }
                }
            }
        }

    }

    public void setOutputPort(int outputPort) {
        this.outputPort = outputPort;
    }

}
