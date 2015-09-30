package com.github.olivereivak.p2md5.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import com.github.olivereivak.p2md5.model.MD5Result;

public class MD5Cracker implements Runnable {

    private static final int END_CHAR = 126;

    private static final int START_CHAR = 32;

    private boolean working;

    private String range;

    private String hash;

    private MessageDigest messageDigest;

    private BlockingQueue<MD5Result> results = new LinkedBlockingQueue<>();

    @Override
    public void run() {

        long start = System.nanoTime();

        work(range);

        double duration = (System.nanoTime() - start) / 1000000000;
        System.out.println("Duration: " + (duration) + " s");
        int wildcardCount = StringUtils.countMatches(range, '?');
        double hashes = Math.pow(END_CHAR - START_CHAR - 1, wildcardCount);
        System.out.println("Hashes: " + hashes);
        double hps = hashes / duration;
        System.out.println(hps + " hashes/second");

    }

    public MD5Cracker(BlockingQueue<MD5Result> results) {
        this.results = results;

        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            messageDigest = null;
        }
    }

    public void work(String range) {
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
            String hashValue = hash(range);
            if (hashValue.equals(hash)) {
                MD5Result result = new MD5Result();
                result.setRange(this.range);
                result.setHash(this.hash);
                result.setMatch(range);
                results.add(result);
            }
            System.out.println(range + " = " + hashValue);
        }
    }

    private String hash(String password) {
        messageDigest.update(password.getBytes());
        return Hex.encodeHexString(messageDigest.digest());
    }

    public boolean isWorking() {
        return working;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

}
