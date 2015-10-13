package com.github.olivereivak.p2md5.service;

import static com.github.olivereivak.p2md5.App.MAX_WORK_IN_QUEUE;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.collections4.map.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.Peer;
import com.github.olivereivak.p2md5.model.protocol.AnswerMD5;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;
import com.github.olivereivak.p2md5.model.protocol.ResourceReply;
import com.github.olivereivak.p2md5.utils.HttpUtils;
import com.github.olivereivak.p2md5.utils.JsonUtils;

public class RequestProcessor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(RequestProcessor.class);

    private static final SecureRandom random = new SecureRandom();

    private BlockingQueue<HttpRequest> requestQueue;
    private BlockingQueue<HttpRequest> outgoingQueue;
    private BlockingQueue<AnswerMD5> arrivedResults;
    private BlockingQueue<ResourceReply> arrivedResourceReplies;
    private BlockingQueue<CheckMD5> work;
    private List<Peer> peers;

    private int outputPort;

    public RequestProcessor(BlockingQueue<HttpRequest> requestQueue, BlockingQueue<HttpRequest> outgoingQueue,
            BlockingQueue<AnswerMD5> arrivedResults, BlockingQueue<ResourceReply> arrivedResourceReplies,
            BlockingQueue<CheckMD5> work, List<Peer> peers) {
        this.requestQueue = requestQueue;
        this.outgoingQueue = outgoingQueue;
        this.arrivedResults = arrivedResults;
        this.arrivedResourceReplies = arrivedResourceReplies;
        this.work = work;
        this.peers = peers;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                processRequest(requestQueue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processRequest(HttpRequest request) throws InterruptedException {
        logger.debug("Processing request {} {}", request.getMethod(), request.getPath());

        MultiValueMap<String, String> queryParams = HttpUtils.parseQueryParams(request.getPath());

        if (request.getMethod().equals("GET")) {
            // Get first query string
            String query = request.getPath().substring(0, request.getPath().indexOf("?"));

            switch (query) {
                case "/resource":
                    processResourceRequest(request, queryParams);
                    break;
            }
        }

        if (request.getMethod().equals("POST")) {
            switch (request.getPath()) {
                case "/checkmd5":
                    processCheckRequest(request);
                    break;
                case "/resourcereply":
                    processResourceReply(request, queryParams);
                    break;
                case "/answermd5":
                    processAnswer(request);
                    break;
            }
        }
    }

    private void processResourceRequest(HttpRequest request, MultiValueMap<String, String> queryParams)
            throws InterruptedException {
        Optional<String> sendip = queryParams.getCollection("sendip").stream().findFirst();
        Optional<String> sendport = queryParams.getCollection("sendport").stream().findFirst();
        Optional<String> ttl = queryParams.getCollection("ttl").stream().findFirst();
        Optional<String> id = queryParams.getCollection("id").stream().findFirst();
        Collection<String> noask = queryParams.getCollection("noask");

        if (!request.getIp().getHostAddress().equals(sendip.get())) {
            logger.trace("ResourceRequest ip addresses do not match. Actual:{} Sent:{}",
                    request.getIp().getHostAddress(), sendip.get());
        }

        if (request.getPort() != Integer.valueOf(sendport.get())) {
            logger.trace("ResourceRequest ports do not match. Actual:{} Sent:{}", request.getPort(), sendport.get());
        }

        if (work.size() < MAX_WORK_IN_QUEUE) {
            // TODO: atm using sender ip, but port from json
            HttpRequest response = new HttpRequest("POST", request.getIp(), Integer.valueOf(sendport.get()),
                    "/resourcereply", "1.0");
            ResourceReply resourceReply = new ResourceReply(HttpUtils.getIp(), outputPort, id.orElse(""), 100);
            response.setBody(JsonUtils.toJson(resourceReply));
            outgoingQueue.put(response);
        }

        if (ttl.isPresent() && Integer.valueOf(ttl.get()) > 1) {
            int newTTL = Integer.valueOf(ttl.get()) - 1;

            // TODO: test this
            MultiValueMap<String, String> forwardParams = queryParams;
            forwardParams.remove("ttl");
            forwardParams.put("ttl", String.valueOf(newTTL));

            Collection<String> forwardNoAsk = forwardParams.getCollection("noask");
            forwardNoAsk.add(HttpUtils.getIp() + "_" + outputPort);

            String path = "/resource?" + HttpUtils.queryParamsToString(forwardParams);

            // Broadcast to all known ip's except those in noask parameters.
            synchronized (peers) {
                for (Peer peer : peers) {
                    if (!peer.isReachable()) {
                        continue;
                    }

                    if (!noask.contains(peer.getIp() + "_" + peer.getPort())) {
                        try {
                            HttpRequest forward;
                            forward = new HttpRequest("GET", InetAddress.getByName(peer.getIp()), peer.getPort(), path,
                                    "1.0");
                            outgoingQueue.put(forward);
                        } catch (UnknownHostException e) {
                            logger.error("Failed to get InetAddress.", e);
                        }
                    }
                }
            }

        }

    }

    private void processResourceReply(HttpRequest request, MultiValueMap<String, String> queryParams) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String body = request.getBody();
            ResourceReply resourceReply = mapper.readValue(body, ResourceReply.class);
            arrivedResourceReplies.add(resourceReply);
            logger.debug("Received ResourceReply.");
        } catch (Exception e) {
            logger.error("Error mapping resourceReply request to ResourceReply class.");
        }
    }

    private void processAnswer(HttpRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String body = request.getBody();
            AnswerMD5 answerMD5 = mapper.readValue(body, AnswerMD5.class);
            arrivedResults.add(answerMD5);
            logger.debug("Received answerMD5.");
        } catch (Exception e) {
            logger.error("Error mapping answermd5 request to AnswerMD5 class.");
        }
    }

    private void processCheckRequest(HttpRequest request) {
        if (work.size() >= MAX_WORK_IN_QUEUE) {
            logger.info("Discarding checkmd5 request, work queue full.");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            String body = request.getBody();
            CheckMD5 checkMD5 = mapper.readValue(body, CheckMD5.class);
            work.add(checkMD5);
            logger.debug("Added work from CheckMD5 request.");
        } catch (Exception e) {
            logger.error("Error mapping checkmd5 request to CheckMD5 class.");
        }
    }

    public void setOutputPort(int outputPort) {
        this.outputPort = outputPort;
    }

}
