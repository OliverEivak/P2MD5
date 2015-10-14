package com.github.olivereivak.p2md5.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.easymock.EasyMockRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.Peer;
import com.github.olivereivak.p2md5.model.protocol.AnswerMD5;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;
import com.github.olivereivak.p2md5.model.protocol.ResourceReply;

@RunWith(EasyMockRunner.class)
public class RequestProcessorTest {

    private BlockingQueue<HttpRequest> arrivedRequests = new LinkedBlockingQueue<>();
    private BlockingQueue<HttpRequest> outgoingRequests = new LinkedBlockingQueue<>();
    private BlockingQueue<AnswerMD5> arrivedResults = new LinkedBlockingQueue<>();
    private BlockingQueue<ResourceReply> arrivedResourceReplies = new LinkedBlockingQueue<>();
    private BlockingQueue<CheckMD5> work = new LinkedBlockingQueue<>();
    private List<Peer> peers = Collections.synchronizedList(new ArrayList<>());

    private RequestProcessor requestProcessor;
    private Thread thread;

    @Before
    public void startThread() {
        requestProcessor = new RequestProcessor(arrivedRequests, outgoingRequests, arrivedResults,
                arrivedResourceReplies, work, peers);

        thread = new Thread(requestProcessor);
        thread.setName("request-processor");
        thread.setDaemon(true);
        thread.start();
    }

    @After
    public void stopThread() {
        thread.interrupt();
    }

    @Test
    public void test() {
        // test
    }

}
