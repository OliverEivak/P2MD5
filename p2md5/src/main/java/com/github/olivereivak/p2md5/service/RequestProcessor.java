package com.github.olivereivak.p2md5.service;

import static com.github.olivereivak.p2md5.App.MAX_WORK_IN_QUEUE;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.collections4.map.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.Peer;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;
import com.github.olivereivak.p2md5.model.protocol.ResourceReply;

public class RequestProcessor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(RequestProcessor.class);

    private static final SecureRandom random = new SecureRandom();

    private BlockingQueue<HttpRequest> requestQueue;
    private BlockingQueue<HttpRequest> outgoingQueue;
    private BlockingQueue<CheckMD5> work;
    private List<Peer> peers;

    private int outputPort;

    public RequestProcessor(BlockingQueue<HttpRequest> requestQueue, BlockingQueue<HttpRequest> outgoingQueue,
            BlockingQueue<CheckMD5> work, List<Peer> peers) {
        this.requestQueue = requestQueue;
        this.outgoingQueue = outgoingQueue;
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

        MultiValueMap<String, String> queryParams = parseQueryParams(request.getPath());

        if (request.getMethod().equals("GET")) {
            switch (request.getPath()) {
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

        if (!request.getIp().equals(sendip.get())) {
            logger.warn("ResourceRequest ip addresses do not match. Actual:{} Sent:{}", request.getIp(), sendip);
        }

        if (request.getPort() != Integer.valueOf(sendport.get())) {
            logger.warn("ResourceRequest ports do not match. Actual:{} Sent:{}", request.getPort(), sendport);
        }

        if (work.size() < MAX_WORK_IN_QUEUE) {
            HttpRequest response = new HttpRequest("POST", request.getIp(), request.getPort(), "/resourcereply", "1.0");
            ResourceReply resourceReply = new ResourceReply(getIp(), outputPort, id.orElse(""), 100);
            response.setBody(getJson(resourceReply));
            outgoingQueue.put(response);
        }

        if (ttl.isPresent() && Integer.valueOf(ttl.get()) > 1) {
            int newTTL = Integer.valueOf(ttl.get()) - 1;

            // TODO: test this
            MultiValueMap<String, String> forwardParams = queryParams;
            Collection<String> forwardTTL = forwardParams.getCollection("ttl");
            forwardTTL = Arrays.asList(String.valueOf(newTTL));

            Collection<String> forwardNoAsk = forwardParams.getCollection("noask");
            forwardNoAsk.add(getIp() + "_" + outputPort);

            String path = "/resource?" + queryParamsToString(forwardParams);

            // Broadcast to all known ip's except those in noask parameters.
            synchronized (peers) {
                for (Peer peer : peers) {
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
        } catch (Exception e) {
            logger.error("Error mapping checkmd5 request to CheckMD5 class.");
        }
    }

    /**
     * Protected for testing purposes.
     */
    protected MultiValueMap<String, String> parseQueryParams(String uri) {
        MultiValueMap<String, String> queryParams = new MultiValueMap<>();
        if (uri == null) {
            return queryParams;
        }

        String[] buffer = uri.split("\\?", 2);

        if (buffer.length == 2) {
            String[] keyValuePairs = buffer[1].split("&");

            for (String keyValuePair : keyValuePairs) {
                String tokens[] = keyValuePair.split("=", 2);
                if (tokens.length == 2) {
                    queryParams.put(tokens[0], tokens[1]);
                } else if (tokens.length == 1) {
                    queryParams.put(tokens[0], "");
                }
            }
        }

        return queryParams;
    }

    /**
     * Protected for testing purposes.
     */
    protected String queryParamsToString(MultiValueMap<String, String> queryParams) {
        String queryString = "";

        for (String key : queryParams.keySet()) {
            Collection<String> values = queryParams.getCollection(key);
            for (String value : values) {
                queryString += key + "=" + value + "&";
            }
        }

        return queryString.substring(0, queryString.length() - 1);
    }

    private String getJson(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        String json = "";
        try {
            json = mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.error("Error serializing object. ", e);
        }
        return json;
    }

    private String getIp() {
        try {
            return InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            logger.error("Error getting ip. ", e);
            return "";
        }
    }

    public void setOutputPort(int outputPort) {
        this.outputPort = outputPort;
    }

}
