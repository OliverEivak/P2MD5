package com.github.olivereivak.p2md5.model.protocol;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class ResourceReply {

    private String ip;

    @JsonSerialize(using = ToStringSerializer.class)
    private int port;

    private String id;

    private int resource;

    public ResourceReply() {

    }

    public ResourceReply(String ip, int port, String id, int resource) {
        super();
        this.ip = ip;
        this.port = port;
        this.id = id;
        this.resource = resource;
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

    public int getResource() {
        return resource;
    }

    public void setResource(int resource) {
        this.resource = resource;
    }

}
