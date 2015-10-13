package com.github.olivereivak.p2md5.model.protocol;

import java.util.List;

import org.apache.commons.collections4.map.MultiValueMap;

import com.github.olivereivak.p2md5.utils.HttpUtils;

public class ResourceRequest {

    private String sendIp;

    private int sendPort;

    private int ttl;

    private String id;

    private List<String> noask;

    public ResourceRequest() {

    }

    public String toString() {
        MultiValueMap<String, String> queryParams = new MultiValueMap<>();
        queryParams.put("sendip", sendIp);
        queryParams.put("sendport", String.valueOf(sendPort));
        queryParams.put("ttl", String.valueOf(ttl));
        queryParams.put("id", id);
        for (String item : noask) {
            queryParams.put("noask", item);
        }
        return "/resource?" + HttpUtils.queryParamsToString(queryParams);
    }

    public String getSendIp() {
        return sendIp;
    }

    public void setSendIp(String sendIp) {
        this.sendIp = sendIp;
    }

    public int getSendPort() {
        return sendPort;
    }

    public void setSendPort(int sendPort) {
        this.sendPort = sendPort;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getNoask() {
        return noask;
    }

    public void setNoask(List<String> noask) {
        this.noask = noask;
    }

}
