package com.github.olivereivak.p2md5.service;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.Peer;
import com.github.olivereivak.p2md5.model.protocol.AnswerMD5;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;
import com.github.olivereivak.p2md5.model.protocol.ResourceReply;
import com.github.olivereivak.p2md5.model.protocol.ResourceRequest;
import com.github.olivereivak.p2md5.utils.HttpUtils;
import com.github.olivereivak.p2md5.utils.JsonUtils;

public class TaskManager implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(TaskManager.class);

    private static final SecureRandom random = new SecureRandom();

    private static final int MAX_WILDCARDS = 3;

    private String hash;
    private AnswerMD5 match;

    private List<Peer> peers;
    private Map<String, String> ranges = new HashMap<String, String>();
    private int totalRanges;

    private BlockingQueue<HttpRequest> outgoingRequests;

    private BlockingQueue<ResourceReply> arrivedResourceReplies;
    private BlockingQueue<AnswerMD5> arrivedResults;

    private int outputPort;

    public TaskManager(String hash, List<Peer> peers, BlockingQueue<HttpRequest> outgoingRequests,
            BlockingQueue<ResourceReply> arrivedResourceReplies, BlockingQueue<AnswerMD5> arrivedResults) {
        super();
        this.hash = hash;
        this.peers = peers;
        this.outgoingRequests = outgoingRequests;
        this.arrivedResourceReplies = arrivedResourceReplies;
        this.arrivedResults = arrivedResults;
    }

    @Override
    public void run() {
        // Create work
        createRanges("?", MAX_WILDCARDS);
        createRanges("??", MAX_WILDCARDS);
        createRanges("???", MAX_WILDCARDS);
        createRanges("????", MAX_WILDCARDS);

        totalRanges = ranges.size();
        // For a range with MAX_WILDCARDS amount of wildcards
        double hashesPerRange = Math.pow(MD5Cracker.END_CHAR - MD5Cracker.START_CHAR - 1, MAX_WILDCARDS);

        long startTime = System.nanoTime();

        try {

            int i = 0;

            while (true) {
                // Send resource requests
                sendResourceRequests();

                // Process arrived resource replies and send work
                while (!arrivedResourceReplies.isEmpty()) {
                    processResourceReply(arrivedResourceReplies.take());
                }

                // Process results
                while (!arrivedResults.isEmpty()) {
                    processResult(arrivedResults.take());
                }

                // Check if match was found
                if (match != null) {
                    logger.info("Match found! " + match.getMatch());
                    break;
                }

                // Check if work is done
                if (ranges.size() == 0) {
                    logger.info("No match found. TaskManager is stopping.");
                    break;
                }

                // Report progress
                double interval = (System.nanoTime() - startTime) / 1000000000;
                if (interval > 2) {
                    int done = totalRanges - ranges.size();
                    logger.info("Checked {} of {} ranges or {} of {} hashes. ", done, totalRanges,
                            hashesPerRange * done, hashesPerRange * totalRanges);
                    startTime = System.nanoTime();
                }

                // Reset peer reachable status once in a while
                i++;
                if (i > 20 * 60) { // ~ once a minute
                    resetPeerReachableStatuses();
                    i = 0;
                }

                // Sleep
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void createRanges(String range, int maximumWildcards) {
        int wildcardCount = StringUtils.countMatches(range, '?');
        if (wildcardCount > maximumWildcards) {
            for (int i = MD5Cracker.START_CHAR; i < MD5Cracker.END_CHAR; i++) {
                if (i != 63) {
                    String newRange = range.replaceFirst("\\?", Matcher.quoteReplacement(Character.toString((char) i)));
                    createRanges(newRange, maximumWildcards);
                }
            }
        } else {
            ranges.put(new BigInteger(130, random).toString(32), range);
        }
    }

    private void sendResourceRequests() {
        synchronized (peers) {
            for (Peer peer : peers) {
                if (!peer.isReachable()) {
                    continue;
                }

                ResourceRequest resourceRequest = new ResourceRequest();
                resourceRequest.setId(new BigInteger(130, random).toString(32));
                String ip = HttpUtils.getIp();
                resourceRequest.setSendIp(ip);
                resourceRequest.setSendPort(outputPort);
                resourceRequest.setTtl(15);
                resourceRequest.setNoask(Arrays.asList(ip + "_" + outputPort));

                try {
                    HttpRequest request = new HttpRequest("GET", InetAddress.getByName(peer.getIp()), peer.getPort(),
                            resourceRequest.toString(), "HTTP/1.0");
                    outgoingRequests.put(request);
                } catch (UnknownHostException e) {
                    logger.error("Failed to get InetAddress for ResourceRequest receiver.", e);
                } catch (InterruptedException e) {
                    logger.error("Failed to put HttpRequest containing ResourceRequest into queue. ", e);
                    Thread.currentThread().interrupt();
                }

            }
        }
    }

    private void processResourceReply(ResourceReply resourceReply) {
        if (resourceReply.getResource() > 0 && !ranges.isEmpty()) {
            sendWork(resourceReply.getIp(), resourceReply.getPort());
        }
    }

    private void sendWork(String ip, int port) {
        int randomIndex = random.nextInt(ranges.size());
        String key = String.valueOf(ranges.keySet().toArray()[randomIndex]);
        String range = ranges.get(key);
        CheckMD5 checkMD5 = new CheckMD5(HttpUtils.getIp(), outputPort, key, hash, Arrays.asList(range));

        try {
            HttpRequest request = new HttpRequest("POST", InetAddress.getByName(ip), port, "/checkmd5", "HTTP/1.0");
            request.setBody(JsonUtils.toJson(checkMD5));
            outgoingRequests.put(request);
        } catch (UnknownHostException e) {
            logger.error("Failed to get InetAddress for CheckMD5 receiver. ", e);
        } catch (InterruptedException e) {
            logger.error("Failed to put HttpRequest containing CheckMD5 into queue. ", e);
            Thread.currentThread().interrupt();
        }
    }

    private void processResult(AnswerMD5 result) {
        if (result != null) {
            if (result.getResult() == AnswerMD5.RESULT_FOUND) {
                // TODO: check result
                if (ranges.containsKey(result.getId())) {
                    match = result;
                } else {
                    logger.warn("Received RESULT_FOUND answer, but ID does not match any ranges. ");
                }
            } else if (result.getResult() == AnswerMD5.RESULT_NOT_FOUND) {
                if (ranges.containsKey(result.getId())) {
                    ranges.remove(result.getId());
                } else {
                    logger.warn("Received RESULT_NOT_FOUND answer, but ID does not match any ranges. ");
                }
            }
        }
    }

    private void resetPeerReachableStatuses() {
        synchronized (peers) {
            for (Peer peer : peers) {
                if (!peer.isReachable()) {
                    peer.setReachable(true);
                }
            }
        }
    }

    public void setOutputPort(int outputPort) {
        this.outputPort = outputPort;
    }

}
