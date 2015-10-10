package com.github.olivereivak.p2md5.model;

public class MD5Result {

    private String ip;

    private int port;

    private String id;

    private String hash;

    private String match;

    public MD5Result(String ip, int port, String id, String hash, String match) {
        super();
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.hash = hash;
        this.match = match;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

}
