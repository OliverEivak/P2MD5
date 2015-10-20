package com.github.olivereivak.p2md5;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
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
import com.github.olivereivak.p2md5.model.protocol.ResourceReply;
import com.github.olivereivak.p2md5.server.HttpRequestSender;
import com.github.olivereivak.p2md5.server.SimpleHttpServer;
import com.github.olivereivak.p2md5.service.CommandListener;
import com.github.olivereivak.p2md5.service.MD5Cracker;
import com.github.olivereivak.p2md5.service.PeerService;
import com.github.olivereivak.p2md5.service.RequestProcessor;
import com.github.olivereivak.p2md5.service.TaskManager;
import com.github.olivereivak.p2md5.utils.HttpUtils;
import com.github.olivereivak.p2md5.utils.JsonUtils;

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

    /**
     * Local work and results to send out.
     */
    private BlockingQueue<CheckMD5> work = new LinkedBlockingQueue<>();
    private BlockingQueue<AnswerMD5> results = new LinkedBlockingQueue<>();

    private BlockingQueue<AnswerMD5> arrivedResults = new LinkedBlockingQueue<>();
    private BlockingQueue<ResourceReply> arrivedResourceReplies = new LinkedBlockingQueue<>();

    private SimpleHttpServer simpleHttpServer;
    private RequestProcessor requestProcessor;

    private List<Peer> peers = Collections.synchronizedList(new ArrayList<>());

    /*
     * For showing time spent not working.
     */
    private double idleStartTime = 0;
    private boolean startedWork = false;
    private int idleMessageCount = 0;

    public static void main(String[] args) {
        App app = new App();
        app.init();
    }

    private void init() {
        logger.info("Starting application.");

        startCommandListener();
        startRequestProcessor();

        peers.addAll(PeerService.getPeersFromFile("machines.txt"));
        peers.addAll(PeerService.getPeersFromURL(MACHINES_URL));

        try {
            run();
        } catch (InterruptedException e) {
            logger.info("Stopping application.");
        }
    }

    private void run() throws InterruptedException {

        while (true) {
            // Execute commands
            while (!commandQueue.isEmpty()) {
                executeCommand(commandQueue.take());
            }

            // Send requests
            while (!outgoingRequests.isEmpty()) {
                startRequestSender(outgoingRequests.take());
            }

            // Check if all workers are alive
            workers = workers.stream().filter(worker -> worker.isAlive()).collect(Collectors.toList());

            // Start new workers
            while (!work.isEmpty() && workers.size() < MAX_WORKERS) {
                startMD5Cracker(work.take());
                startedWork = true;
            }

            calculateIdleTime();

            // Send results back
            while (!results.isEmpty()) {
                AnswerMD5 result = results.take();
                logger.debug("Sending result: {}", result.getMatch());
                sendAnswer(result);
            }

            Thread.sleep(50);
        }
    }

    private void calculateIdleTime() {
        if (startedWork) {
            if (workers.size() < MAX_WORKERS && idleStartTime == 0) {
                idleStartTime = System.nanoTime();
            } else if (idleStartTime != 0) {
                logger.info("(Partially) idle for {} ms.", (System.nanoTime() - idleStartTime) / 1000000);
                idleStartTime = 0;
                idleMessageCount++;
            }

            if (idleMessageCount > 10) {
                logger.info("Idle.");
                startedWork = false;
                idleMessageCount = 0;
            }
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
        requestProcessor = new RequestProcessor(arrivedRequests, outgoingRequests, arrivedResults,
                arrivedResourceReplies, work, peers);
        newThread(requestProcessor, "request-processor");
    }

    private void startRequestSender(HttpRequest httpRequest) {
        HttpRequestSender requestSender = new HttpRequestSender(httpRequest, peers);
        requestSender.setOutputPort(simpleHttpServer.getPort());
        newThread(requestSender, "request-sender");
    }

    private void startMD5Cracker(CheckMD5 checkMD5) {
        MD5Cracker md5Cracker = new MD5Cracker(results, checkMD5);
        Thread thread = newThread(md5Cracker, getMD5CrackerThreadName());
        workers.add(thread);
    }

    private void startTaskManager(String hash) {
        TaskManager taskManager = new TaskManager(hash, peers, outgoingRequests, arrivedResourceReplies,
                arrivedResults);
        taskManager.setOutputPort(simpleHttpServer.getPort());
        newThread(taskManager, "task-manager");
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
                    "/answermd5", "HTTP/1.0");
            result.setIp(HttpUtils.getIp());
            result.setPort(simpleHttpServer.getPort());
            request.setBody(JsonUtils.toJson(result));
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
                    logger.info("Starting TaskManager.");
                    String hash = command.getParameters().get(0);
                    startTaskManager(hash);
                }
                break;
        }
    }

    private Thread newThread(Runnable target, String name) {
        logger.trace("Starting {}", name);
        Thread thread = new Thread(target);
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

}
