package com.github.olivereivak.p2md5.service;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.easymock.EasyMockRunner;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.olivereivak.p2md5.model.MD5Result;

@RunWith(EasyMockRunner.class)
public class MD5CrackerTest {

    private BlockingQueue<MD5Result> results = new LinkedBlockingQueue<>();

    @TestSubject
    MD5Cracker md5Cracker = new MD5Cracker(results);

    @Test
    public void work() {
        long start = System.nanoTime();
        md5Cracker.work("???");
        long duration = System.nanoTime() - start;
        System.out.println("Duration: " + (duration / 1000000000) + " s");
    }

    @Test
    public void test() throws InterruptedException {
        String range = "???";
        String hash = "a92c97fe71775f00f4e826e2852fc511";
        String match = "}}y";

        md5Cracker.setRange(range);
        md5Cracker.setHash(hash);

        Thread thread = new Thread(md5Cracker);
        thread.setName("md5-cracker");
        thread.setDaemon(true);
        thread.start();
        thread.join();

        assertEquals(1, results.size());
        MD5Result result = results.take();
        assertEquals(range, result.getRange());
        assertEquals(hash, result.getHash());
        assertEquals(match, result.getMatch());
    }

}
