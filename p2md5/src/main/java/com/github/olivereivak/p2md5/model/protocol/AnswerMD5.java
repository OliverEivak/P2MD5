package com.github.olivereivak.p2md5.model.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class AnswerMD5 {

    public static final int RESULT_FOUND = 0;
    public static final int RESULT_NOT_FOUND = 1;
    public static final int RESULT_NOT_CALCULATED = 2;

    private String ip;

    @JsonSerialize(using = ToStringSerializer.class)
    // TODO: deserialize to string
    private int port;

    private String id;

    @JsonProperty("md5")
    private String hash;

    private int result;

    @JsonProperty("resultstring")
    private String match;

    public AnswerMD5(String ip, int port, String id, String hash, String match) {
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

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

}
