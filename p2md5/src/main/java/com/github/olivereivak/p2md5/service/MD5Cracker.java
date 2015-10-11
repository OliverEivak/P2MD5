package com.github.olivereivak.p2md5.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.olivereivak.p2md5.model.MD5Result;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;

public class MD5Cracker implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(MD5Cracker.class);

    private static final int END_CHAR = 126;

    private static final int START_CHAR = 32;

    private boolean working;

    private String match = null;

    private long startTime;

    private double hashesTotal = 0;

    private double hashesDone;

    private MessageDigest messageDigest;

    private BlockingQueue<MD5Result> results = new LinkedBlockingQueue<>();

    private CheckMD5 checkMD5;

    public MD5Cracker(BlockingQueue<MD5Result> results, CheckMD5 checkMD5) {
        this.results = results;
        this.checkMD5 = checkMD5;

        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            messageDigest = null;
        }
    }

    @Override
    public void run() {
        startTime = System.nanoTime();

        working = true;
        for (String range : checkMD5.getRanges()) {
            logger.debug("range={} hash={}", range, checkMD5.getMd5());

            int wildcardCount = StringUtils.countMatches(range, '?');
            hashesTotal += Math.pow(END_CHAR - START_CHAR - 1, wildcardCount);

            work(range);
        }
        saveMatch();
        working = false;

        printProgress();
        logger.debug("Finished.");
    }

    private void printProgress() {
        double duration = (System.nanoTime() - startTime) / 1000000000;
        double hps = hashesDone / duration;
        logger.debug("{}/{} time={} hps={}", hashesDone, hashesTotal, duration, hps);
    }

    public void work(String range) {
        // Stop working if match is found
        if (match != null) {
            return;
        }

        int wildcardCount = StringUtils.countMatches(range, '?');
        if (wildcardCount > 0) {
            for (int i = START_CHAR; i < END_CHAR; i++) {
                if (i == 63) {
                    continue;
                }
                String newRange = range.replaceFirst("\\?", Matcher.quoteReplacement(Character.toString((char) i)));
                work(newRange);
            }
        } else {
            hashesDone++;
            if (hashesDone % 1000000 == 0) {
                printProgress();
            }

            String hashValue = hash(range);
            if (hashValue.equals(checkMD5.getMd5())) {
                match = range;
            }
        }
    }

    private String hash(String password) {
        messageDigest.update(password.getBytes());
        return Hex.encodeHexString(messageDigest.digest());
    }

    private void saveMatch() {
        if (match != null) {
            MD5Result result = new MD5Result(checkMD5.getIp(), checkMD5.getPort(), checkMD5.getId(), checkMD5.getMd5(),
                    match);
            results.add(result);
        }
    }

    public boolean isWorking() {
        return working;
    }

}
