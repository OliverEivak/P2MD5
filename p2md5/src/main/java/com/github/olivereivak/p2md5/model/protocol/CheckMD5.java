package com.github.olivereivak.p2md5.model.protocol;

import java.util.List;

public class CheckMD5 {

    private String ip;

    private int port;

    private String id;

    private String md5;

    private List<String> ranges;

    public CheckMD5(String ip, int port, String id, String md5, List<String> ranges) {
        super();
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.md5 = md5;
        this.ranges = ranges;
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

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public List<String> getRanges() {
        return ranges;
    }

    public void setRanges(List<String> ranges) {
        this.ranges = ranges;
    }

    // TODO
    // private String wildcard;
    // private String symbolRange;

}
