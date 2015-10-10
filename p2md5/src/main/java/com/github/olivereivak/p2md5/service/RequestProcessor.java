package com.github.olivereivak.p2md5.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.collections4.map.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;
import com.github.olivereivak.p2md5.model.protocol.ResourceReply;

public class RequestProcessor implements Runnable {

    private static final SecureRandom random = new SecureRandom();

    private BlockingQueue<HttpRequest> requestQueue;
    private BlockingQueue<HttpRequest> outgoingQueue;
    private BlockingQueue<CheckMD5> work;

    private int outputPort;

    public RequestProcessor(BlockingQueue<HttpRequest> requestQueue, BlockingQueue<HttpRequest> outgoingQueue,
            BlockingQueue<CheckMD5> work) {
        this.requestQueue = requestQueue;
        this.outgoingQueue = outgoingQueue;
        this.work = work;
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
        System.out.println("RequestProcessor: " + request.getMethod() + " " + request.getUri());

        MultiValueMap<String, String> queryParams = parseQueryParams(request.getUri());

        if (request.getMethod().equals("GET")) {
            switch (request.getUri()) {
                case "/resource":
                    processResourceRequest(request, queryParams);
                    break;
            }
        }

        if (request.getMethod().equals("POST")) {
            switch (request.getUri()) {
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

        boolean sendResponse = true;
        boolean sendForward = true;

        // TODO: check if we already have work
        if (sendResponse) {
            HttpRequest response = new HttpRequest("POST", "/resourcereply", "HTTP/1.0");
            response.setIp(request.getIp());
            response.setPort(request.getPort());
            ResourceReply resourceReply = new ResourceReply(getIp(), outputPort, id.orElse(""), 100);
            response.setBody(resourceReply.toJson());

            outgoingQueue.put(response);
            // must add headers when sending
        }

        if (ttl.isPresent() && Integer.valueOf(ttl.get()) > 1) {
            int newTTL = Integer.valueOf(ttl.get()) - 1;

            // TODO: test this
            MultiValueMap<String, String> forwardParams = queryParams;
            Collection<String> forwardTTL = forwardParams.getCollection("ttl");
            forwardTTL = Arrays.asList(String.valueOf(newTTL));

            Collection<String> forwardNoAsk = forwardParams.getCollection("noask");
            forwardNoAsk.add(getIp() + "_" + outputPort);

            String uri = "/resource?" + queryParamsToString(forwardParams);
            HttpRequest forward = new HttpRequest("GET", uri, "HTTP/1.0");

            // TODO: broadcast to all known ip's except those in noask params.
            // outgoingQueue.put(forward);

        }

    }

    private void processCheckRequest(HttpRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        CheckMD5 checkMD5;
        try {
            String body = request.getBody();
            checkMD5 = mapper.readValue(body, CheckMD5.class);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        work.add(checkMD5);
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

    private String getIp() {
        try {
            return InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            System.out.println(e);
            return "";
        }
    }

    public void setOutputPort(int outputPort) {
        this.outputPort = outputPort;
    }

}
