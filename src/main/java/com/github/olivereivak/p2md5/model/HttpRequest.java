package com.github.olivereivak.p2md5.model;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private static final String CRLF = "\r\n";

    private String method;

    private String path;

    private String version;

    private Map<String, String> headers;

    private String body;

    private InetAddress ip;

    private int port;

    public HttpRequest() {

    }

    public HttpRequest(String method, InetAddress ip, int port, String path, String version) {
        super();
        this.method = method;
        this.ip = ip;
        this.port = port;
        this.path = path;
        this.version = version;
        this.headers = new HashMap<>();
    }

    public byte[] getBytes() {
        return toString().getBytes();
    }

    public String toString() {
        if (method.equals("GET")) {
            return getGetRequest();
        } else {
            return getPostRequest();
        }
    }

    private String getPostRequest() {
        String request = "";
        request += method + " " + path + " " + version + " " + CRLF;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request += entry.getKey() + ": " + entry.getValue() + CRLF;
        }
        if (!headers.containsKey("Content-Length")) {
            request += "Content-Length: " + body.length() + CRLF;
        }
        request += CRLF;
        request += body;
        return request;
    }

    private String getGetRequest() {
        String request = "";
        request += method + " " + path + " " + version + " " + CRLF;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request += entry.getKey() + ": " + entry.getValue() + CRLF;
        }
        request += CRLF;
        return request;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public InetAddress getIp() {
        return ip;
    }

    public void setIp(InetAddress ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
