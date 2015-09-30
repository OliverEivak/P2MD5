package com.github.olivereivak.p2md5.service;

import org.easymock.EasyMockRunner;
import org.easymock.TestSubject;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class MD5CrackerTest {

    @TestSubject
    MD5Cracker md5Cracker = new MD5Cracker();

    @Test
    public void work() {
        long start = System.nanoTime();
        md5Cracker.work("???");
        long duration = System.nanoTime() - start;
        System.out.println("Duration: " + (duration / 1000000000) + " s");
    }

    @Test
    public void test() throws InterruptedException {
        md5Cracker.setRange("???");
        Thread thread = new Thread(md5Cracker);
        thread.setName("md5-cracker");
        thread.setDaemon(true);
        thread.start();
        thread.join();
    }

}
