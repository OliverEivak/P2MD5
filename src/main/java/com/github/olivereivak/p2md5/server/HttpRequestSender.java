package com.github.olivereivak.p2md5.server;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.Peer;
import com.github.olivereivak.p2md5.utils.HttpUtils;

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

        // Don't sent messages to yourself
        if (request.getIp().equals(HttpUtils.getIp()) && request.getPort() == outputPort) {
            return;
        }

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(request.getIp(), request.getPort()), 1000);

            OutputStream output = socket.getOutputStream();
            logger.trace(request.toString());
            output.write(request.getBytes());

            output.flush();
            output.close();
            socket.close();
        } catch (Exception e) {
            logger.error("Failed to create socket to " + request.getIp().getHostAddress() + ":" + request.getPort());
            markPeerUnreachable(request.getIp(), request.getPort());
        }

    }

    private void markPeerUnreachable(InetAddress ip, int port) {
        synchronized (peers) {
            for (Peer peer : peers) {
                if (peer.getIp().equals(ip.getHostAddress()) && peer.getPort() == port) {
                    peer.setReachable(false);
                }
            }
        }
    }

    public void setOutputPort(int outputPort) {
        this.outputPort = outputPort;
    }

}
