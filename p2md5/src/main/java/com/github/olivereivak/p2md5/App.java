package com.github.olivereivak.p2md5;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.olivereivak.p2md5.model.Command;
import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.Peer;
import com.github.olivereivak.p2md5.model.protocol.AnswerMD5;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;
import com.github.olivereivak.p2md5.server.HttpRequestSender;
import com.github.olivereivak.p2md5.server.SimpleHttpServer;
import com.github.olivereivak.p2md5.service.CommandListener;
import com.github.olivereivak.p2md5.service.MD5Cracker;
import com.github.olivereivak.p2md5.service.PeerService;
import com.github.olivereivak.p2md5.service.RequestProcessor;

public class App {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static final String MACHINES_URL = "http://dijkstra.cs.ttu.ee/~Oliver.Eivak/machines.txt";

    private static final String COMMAND_EXIT = "exit";
    private static final String COMMAND_START = "start";
    private static final String COMMAND_CRACK = "crack";

    private static final int DEFAULT_PORT = 5678;
    private static final int MAX_WORKERS = 2;
    public static final int MAX_WORK_IN_QUEUE = 2;

    private BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<HttpRequest> arrivedRequests = new LinkedBlockingQueue<>();
    private BlockingQueue<HttpRequest> outgoingRequests = new LinkedBlockingQueue<>();

    private List<Thread> workers = new ArrayList<>();
    private BlockingQueue<CheckMD5> work = new LinkedBlockingQueue<>();
    private BlockingQueue<AnswerMD5> results = new LinkedBlockingQueue<>();

    private SimpleHttpServer simpleHttpServer;
    private RequestProcessor requestProcessor;

    private List<Peer> peers = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        App app = new App();
        app.init();
    }

    private void init() throws InterruptedException {
        startCommandListener();
        startRequestProcessor();
        startRequestSender();

        peers.addAll(PeerService.getPeersFromFile("machines.txt"));
        peers.addAll(PeerService.getPeersFromURL(MACHINES_URL));

        run();
    }

    private void run() throws InterruptedException {
        while (true) {
            // Execute commands
            while (!commandQueue.isEmpty()) {
                executeCommand(commandQueue.take());
            }

            // Check if all workers are alive
            workers = workers.stream().filter(worker -> worker.isAlive()).collect(Collectors.toList());

            // Start new workers
            while (!work.isEmpty() && workers.size() < MAX_WORKERS) {
                startMD5Cracker(work.take());
            }

            // Send results back
            while (!results.isEmpty()) {
                AnswerMD5 result = results.take();
                logger.debug("Sending result: {}", result.getMatch());
                sendAnswer(result);
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
            logger.warn("SimpleHttpServer already running on port {}", simpleHttpServer.getPort());
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
        Thread thread = newThread(md5Cracker, getMD5CrackerThreadName());
        workers.add(thread);
    }

    private String getMD5CrackerThreadName() {
        List<String> usedNames = new ArrayList<>();
        for (Thread worker : workers) {
            usedNames.add(worker.getName());
        }

        for (int i = 1; i <= MAX_WORKERS; i++) {
            if (!usedNames.contains("md5-cracker-" + i)) {
                return "md5-cracker-" + i;
            }
        }
        return "md5-cracker  ";
    }

    private void sendAnswer(AnswerMD5 result) {
        try {
            HttpRequest request = new HttpRequest("POST", InetAddress.getByName(result.getIp()), result.getPort(),
                    "/answermd5", "1.0");
            outgoingRequests.put(request);
        } catch (UnknownHostException e) {
            logger.error("Failed to get InetAddress for request receiver. ", e);
        } catch (InterruptedException e) {
            logger.error("Failed to put request into queue. ", e);
        }
    }

    private void executeCommand(Command command) throws InterruptedException {
        switch (command.getCommand()) {
            case COMMAND_START:
                int port = DEFAULT_PORT;
                if (!command.getParameters().isEmpty()) {
                    port = Integer.valueOf(command.getParameters().get(0));
                }
                logger.info("Starting SimpleHttpServer on port {}", port);
                startSimpleHttpServer(port);
                break;
            case COMMAND_EXIT:
                logger.info("Exiting application.");
                System.exit(0);
                break;
            case COMMAND_CRACK:
                if (!command.getParameters().isEmpty()) {
                    logger.info("Starting to crack md5 hash.");
                    int sendPort = DEFAULT_PORT;
                    if (simpleHttpServer != null) {
                        sendPort = simpleHttpServer.getPort();
                    }
                    CheckMD5 checkMD5 = new CheckMD5("127.0.0.1", sendPort, "1", command.getParameters().get(1),
                            Arrays.asList(command.getParameters().get(0)));
                    work.put(checkMD5);
                }
                break;
        }
    }

    private Thread newThread(Runnable target, String name) {
        logger.info("Starting {}", name);
        Thread thread = new Thread(target);
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

}
