package com.github.olivereivak.p2md5;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.olivereivak.p2md5.model.Command;
import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.server.SimpleHttpServer;
import com.github.olivereivak.p2md5.service.CommandListener;
import com.github.olivereivak.p2md5.service.RequestProcessor;

public class App {

    private static final String COMMAND_EXIT = "exit";
    private static final String COMMAND_START = "start";

    private static final int DEFAULT_PORT = 5678;

    private BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<HttpRequest> arrivedRequests = new LinkedBlockingQueue<>();

    private SimpleHttpServer simpleHttpServer = null;

    public static void main(String[] args) throws InterruptedException {
        App app = new App();
        app.init();
    }

    private void init() throws InterruptedException {
        startCommandListener();
        startRequestProcessor();

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
        } else {
            System.out.println("SimpleHttpServer already running on port " + simpleHttpServer.getPort());
        }
    }

    private void startRequestProcessor() {
        RequestProcessor requestProcessor = new RequestProcessor(arrivedRequests);
        requestProcessor.setOutputPort(simpleHttpServer.getPort());

        Thread thread = new Thread(requestProcessor);
        thread.setName("request-processor");
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
        }
    }

}
