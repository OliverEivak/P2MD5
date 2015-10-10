package com.github.olivereivak.p2md5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import com.github.olivereivak.p2md5.model.Command;
import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.MD5Result;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;
import com.github.olivereivak.p2md5.server.HttpRequestSender;
import com.github.olivereivak.p2md5.server.SimpleHttpServer;
import com.github.olivereivak.p2md5.service.CommandListener;
import com.github.olivereivak.p2md5.service.MD5Cracker;
import com.github.olivereivak.p2md5.service.RequestProcessor;

public class App {

    private static final String COMMAND_EXIT = "exit";
    private static final String COMMAND_START = "start";
    private static final String COMMAND_CRACK = "crack";

    private static final int DEFAULT_PORT = 5678;
    private static final int MAX_WORKERS = 4;

    private BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<HttpRequest> arrivedRequests = new LinkedBlockingQueue<>();
    private BlockingQueue<HttpRequest> outgoingRequests = new LinkedBlockingQueue<>();

    private List<Thread> workers = new ArrayList<>();
    private BlockingQueue<CheckMD5> work = new LinkedBlockingQueue<>();
    private BlockingQueue<MD5Result> results = new LinkedBlockingQueue<>();

    private SimpleHttpServer simpleHttpServer = null;
    private RequestProcessor requestProcessor = null;

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
            // Execute commands
            if (!commandQueue.isEmpty()) {
                executeCommand(commandQueue.take());
            }

            // Check if all workers are alive
            workers = workers.stream().filter(worker -> worker.isAlive()).collect(Collectors.toList());

            // Start new workers
            if (!work.isEmpty() && workers.size() < MAX_WORKERS) {
                startMD5Cracker(work.take());
            }

            // Show results -> TODO: send results back
            if (!results.isEmpty()) {
                MD5Result result = results.take();
                System.out.println("Match: " + result.getMatch());
            }

            Thread.sleep(100);
        }
    }

    private void startCommandListener() {
        CommandListener commandListener = new CommandListener(commandQueue);
        newThread(commandListener, "command-listener");
    }

    private void startSimpleHttpServer(int port) {
        if (simpleHttpServer == null) {
            simpleHttpServer = new SimpleHttpServer(port, arrivedRequests);
            newThread(simpleHttpServer, "http-server");

            requestProcessor.setOutputPort(simpleHttpServer.getPort());
        } else {
            System.out.println("SimpleHttpServer already running on port " + simpleHttpServer.getPort());
        }
    }

    private void startRequestProcessor() {
        requestProcessor = new RequestProcessor(arrivedRequests, outgoingRequests, work);
        newThread(requestProcessor, "request-processor");
    }

    private void startRequestSender() {
        HttpRequestSender requestSender = new HttpRequestSender(outgoingRequests);
        newThread(requestSender, "request-sender");
    }

    private void startMD5Cracker(CheckMD5 checkMD5) {
        MD5Cracker md5Cracker = new MD5Cracker(results, checkMD5);
        Thread thread = newThread(md5Cracker, "md5-cracker-" + (workers.size() + 1));
        workers.add(thread);
    }

    private void executeCommand(Command command) {
        switch (command.getCommand()) {
            case COMMAND_START:
                int port = DEFAULT_PORT;
                if (!command.getParameters().isEmpty()) {
                    port = Integer.valueOf(command.getParameters().get(0));
                }
                System.out.println("Starting SimpleHttpServer on port " + port + ".");
                startSimpleHttpServer(port);
                break;
            case COMMAND_EXIT:
                System.out.println("Exiting application. ");
                System.exit(0);
                break;
            case COMMAND_CRACK:
                if (!command.getParameters().isEmpty()) {
                    System.out.println("Starting to crack md5 hash.");
                    int sendPort = DEFAULT_PORT;
                    if (simpleHttpServer != null) {
                        sendPort = simpleHttpServer.getPort();
                    }
                    CheckMD5 checkMD5 = new CheckMD5("127.0.0.1", sendPort, "1", command.getParameters().get(1),
                            Arrays.asList(command.getParameters().get(0)));
                    startMD5Cracker(checkMD5);
                }
                break;
        }
    }

    private Thread newThread(Runnable target, String name) {
        Thread thread = new Thread(target);
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

}
