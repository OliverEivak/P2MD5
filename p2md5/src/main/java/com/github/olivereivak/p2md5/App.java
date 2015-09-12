package com.github.olivereivak.p2md5;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.olivereivak.p2md5.model.Command;
import com.github.olivereivak.p2md5.server.SimpleHttpServer;

public class App {

    private static final String COMMAND_EXIT = "exit";
    private static final String COMMAND_START = "start";

    private static final int DEFAULT_PORT = 5678;

    BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws InterruptedException {
        App app = new App();
        app.init();
    }

    private void init() throws InterruptedException {
        startCommandListener();

        while (true) {
            if (!commandQueue.isEmpty()) {
                executeCommand(commandQueue.take());
            }
        }

    }

    private void startCommandListener() {
        CommandListener runnable = new CommandListener();
        runnable.setCommandQueue(commandQueue);

        Thread commandListener = new Thread(runnable);
        commandListener.setName("command-listener");
        commandListener.setDaemon(true);
        commandListener.start();
    }

    private void startSimpleHttpServer(int port) {
        SimpleHttpServer runnable = new SimpleHttpServer();
        runnable.setPort(port);

        Thread httpServer = new Thread(runnable);
        httpServer.setName("http-server");
        httpServer.setDaemon(true);
        httpServer.start();
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
