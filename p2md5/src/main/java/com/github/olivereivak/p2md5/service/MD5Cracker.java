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

import com.github.olivereivak.p2md5.model.protocol.AnswerMD5;
import com.github.olivereivak.p2md5.model.protocol.CheckMD5;

public class MD5Cracker implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(MD5Cracker.class);

    public static final int START_CHAR = 32;
    public static final int END_CHAR = 126;

    private String match = null;

    private long startTime;

    private double hashesTotal = 0;
    private double hashesDone;

    private MessageDigest messageDigest;

    private BlockingQueue<AnswerMD5> results = new LinkedBlockingQueue<>();

    private CheckMD5 checkMD5;

    public MD5Cracker(BlockingQueue<AnswerMD5> results, CheckMD5 checkMD5) {
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

        for (String range : checkMD5.getRanges()) {
            logger.debug("range={} hash={}", range, checkMD5.getMd5());

            int wildcardCount = StringUtils.countMatches(range, '?');
            hashesTotal += Math.pow(END_CHAR - START_CHAR - 1, wildcardCount);

            work(range);
        }
        createResult();

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
                logger.info("Found match {} for {}", range, hashValue);
                match = range;
            }
        }
    }

    private String hash(String password) {
        messageDigest.update(password.getBytes());
        return Hex.encodeHexString(messageDigest.digest());
    }

    private void createResult() {
        if (match != null) {
            AnswerMD5 result = new AnswerMD5(checkMD5.getIp(), checkMD5.getPort(), checkMD5.getId(), checkMD5.getMd5(),
                    match);
            result.setResult(AnswerMD5.RESULT_FOUND);
            results.add(result);
        } else {
            AnswerMD5 result = new AnswerMD5(checkMD5.getIp(), checkMD5.getPort(), checkMD5.getId(), checkMD5.getMd5(),
                    null);
            result.setResult(AnswerMD5.RESULT_NOT_FOUND);
            results.add(result);
        }
    }

}
