package com.github.olivereivak.p2md5.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Cracker implements Runnable {

    private boolean working;

    private String range;

    private String hash;

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            try {
                crack();
            } catch (Exception e) {
                System.out.println("Error cracking MD5.");
                break;
            }
        }

    }

    private String crack() throws NoSuchAlgorithmException {
        String password = "123456";

        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(password.getBytes());

        byte byteData[] = md.digest();

        StringBuffer sb = new StringBuffer();
        // TODO; use apache commons
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        System.out.println("Digest(in hex format):: " + sb.toString());

        return "";
    }

    public boolean isWorking() {
        return working;
    }

    public void setWorking(boolean working) {
        this.working = working;
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
