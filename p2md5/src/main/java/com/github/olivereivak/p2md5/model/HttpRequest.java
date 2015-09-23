package com.github.olivereivak.p2md5.model;

import java.net.InetAddress;
import java.util.Map;

public class HttpRequest {

    private static final String CRLF = "\r\n";

    private String method;

    private String uri;

    private String version;

    private Map<String, String> headers;

    private String body;

    private InetAddress ip;

    private int port;

    public HttpRequest() {

    }

    public HttpRequest(String method, String uri, String version) {
        super();
        this.method = method;
        this.uri = uri;
        this.version = version;
    }

    public byte[] getBytes() {
        if (method.equals("GET")) {
            return getGetRequest();
        } else {
            return getPostRequest();
        }
    }

    public byte[] getPostRequest() {
        String request = "";
        request += method + " " + uri + "HTTP/1.0 " + CRLF;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request += entry.getKey() + ": " + entry.getValue() + CRLF;
        }
        request += "Content-Length: " + body.length() + CRLF;
        request += CRLF;
        request += body;
        return request.getBytes();
    }

    public byte[] getGetRequest() {
        String request = "";
        request += method + " " + uri + "HTTP/1.0 " + CRLF;
        return request.getBytes();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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
