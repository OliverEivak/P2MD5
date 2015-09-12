package com.github.olivereivak.p2md5.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleHttpServer implements Runnable {

    private int port;

    @Override
    public void run() {

        ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                try {
                    HttpRequestHandler request = new HttpRequestHandler(socket);

                    Thread requestHandler = new Thread(request);
                    requestHandler.setName("http-request-handler");
                    requestHandler.setDaemon(true);
                    requestHandler.start();
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }

    }

    public void setPort(int port) {
        this.port = port;
    }

}
