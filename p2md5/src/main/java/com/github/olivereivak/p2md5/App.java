package com.github.olivereivak.p2md5;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.olivereivak.p2md5.model.Command;
import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.MD5Result;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;
import com.github.olivereivak.p2md5.server.HttpRequestSender;
import com.github.olivereivak.p2md5.server.SimpleHttpServer;
import com.github.olivereivak.p2md5.service.CommandListener;
import com.github.olivereivak.p2md5.service.RequestProcessor;

public class App {

    private static final String COMMAND_EXIT = "exit";
    private static final String COMMAND_START = "start";
    private static final String COMMAND_CRACK = "crack";

    private static final int DEFAULT_PORT = 5678;

    private BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<HttpRequest> arrivedRequests = new LinkedBlockingQueue<>();
    private BlockingQueue<HttpRequest> outgoingRequests = new LinkedBlockingQueue<>();

    private BlockingQueue<CheckMD5> work = new LinkedBlockingQueue<>();
    private BlockingQueue<MD5Result> results = new LinkedBlockingQueue<>();

    private SimpleHttpServer simpleHttpServer = null;
    RequestProcessor requestProcessor = null;

    public static void main(String[] args) throws InterruptedException {
        App app = new App();
        app.init();
    }

    private void init() throws InterruptedException {
        startCommandListener();
        startRequestProcessor();
        startRequestSender();

        run();
    }

    private void run() throws InterruptedException {
        while (true) {
            if (!commandQueue.isEmpty()) {
                executeCommand(commandQueue.take());
            }
            Thread.sleep(100);
        }
    }

    private void startCommandListener() {
        CommandListener commandListener = new CommandListener(commandQueue);

        Thread thread = new Thread(commandListener);
        thread.setName("command-listener");
        thread.setDaemon(true);
        thread.start();
    }

    private void startSimpleHttpServer(int port) {
        if (simpleHttpServer == null) {
            simpleHttpServer = new SimpleHttpServer(port, arrivedRequests);

            Thread thread = new Thread(simpleHttpServer);
            thread.setName("http-server");
            thread.setDaemon(true);
            thread.start();

            requestProcessor.setOutputPort(simpleHttpServer.getPort());
        } else {
            System.out.println("SimpleHttpServer already running on port " + simpleHttpServer.getPort());
        }
    }

    private void startRequestProcessor() {
        requestProcessor = new RequestProcessor(arrivedRequests, outgoingRequests, work);

        Thread thread = new Thread(requestProcessor);
        thread.setName("request-processor");
        thread.setDaemon(true);
        thread.start();
    }

    private void startRequestSender() {
        HttpRequestSender requestSender = new HttpRequestSender(outgoingRequests);

        Thread thread = new Thread(requestSender);
        thread.setName("request-sender");
        thread.setDaemon(true);
        thread.start();
    }

    private void executeCommand(Command command) {
        switch (command.getCommand()) {
            case COMMAND_START:
                int port = DEFAULT_PORT;
                if (!command.getParameter().isEmpty()) {
                    port = Integer.valueOf(command.getParameter());
                }
                System.out.println("Starting SimpleHttpServer on port " + port + ".");
                startSimpleHttpServer(port);
                break;
            case COMMAND_EXIT:
                System.out.println("Exiting application. ");
                System.exit(0);
                break;
            case COMMAND_CRACK:
                if (!command.getParameter().isEmpty()) {
                    System.out.println("Starting to crack md5 hash.");

                }
                break;
        }
    }

}
