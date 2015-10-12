package com.github.olivereivak.p2md5.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.easymock.EasyMockRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.olivereivak.p2md5.model.protocol.AnswerMD5;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;

@RunWith(EasyMockRunner.class)
public class MD5CrackerTest {

    private BlockingQueue<AnswerMD5> results = new LinkedBlockingQueue<>();

    @Test
    public void test() throws InterruptedException {
        String range = "???";
        String hash = "a92c97fe71775f00f4e826e2852fc511";
        String match = "}}y";

        CheckMD5 checkMD5 = new CheckMD5("1.2.3.4", 1234, "1", hash, Arrays.asList(range));

        MD5Cracker md5Cracker = new MD5Cracker(results, checkMD5);

        Thread thread = new Thread(md5Cracker);
        thread.setName("md5-cracker");
        thread.setDaemon(true);
        thread.start();
        thread.join();

        assertEquals(1, results.size());
        AnswerMD5 result = results.take();
        assertEquals(hash, result.getHash());
        assertEquals(match, result.getMatch());
        assertEquals("1.2.3.4", result.getIp());
        assertEquals(1234, result.getPort());
        assertEquals("1", result.getId());
    }

    @Test
    public void testTwoThreads() throws InterruptedException {
        String range = "???";
        String hash = "a92c97fe71775f00f4e826e2852fc511";
        String match = "}}y";

        CheckMD5 checkMD5 = new CheckMD5("1.2.3.4", 1234, "1", hash, Arrays.asList(range));

        MD5Cracker md5Cracker = new MD5Cracker(results, checkMD5);

        Thread thread = new Thread(md5Cracker);
        thread.setName("md5-cracker1");
        thread.setDaemon(true);
        thread.start();

        String range2 = "????";
        String hash2 = "e61e7de603852182385da5e907b4b232";
        String match2 = "hhhh";

        checkMD5 = new CheckMD5("2.3.4.5", 2345, "2", hash2, Arrays.asList(range2));

        md5Cracker = new MD5Cracker(results, checkMD5);

        MD5Cracker md5Cracker2 = new MD5Cracker(results, checkMD5);

        Thread thread2 = new Thread(md5Cracker2);
        thread2.setName("md5-cracker2");
        thread2.setDaemon(true);
        thread2.start();

        thread.join();
        thread2.join();

        assertEquals(2, results.size());

        AnswerMD5 result = results.take();

        if (result.getMatch().equals(match)) {
            assertEquals(hash, result.getHash());
            assertEquals(match, result.getMatch());
            assertEquals("1.2.3.4", result.getIp());
            assertEquals(1234, result.getPort());
            assertEquals("1", result.getId());
        } else if (result.getMatch().equals(match2)) {
            assertEquals(hash2, result.getHash());
            assertEquals(match2, result.getMatch());
            assertEquals("2.3.4.5", result.getIp());
            assertEquals(2345, result.getPort());
            assertEquals("2", result.getId());
        } else {
            fail("Wrong match");
        }

    }

}
