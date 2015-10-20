package com.github.olivereivak.p2md5.model;

import java.util.Map;

public class HttpResponse {

    public static final String HTTP_OK = "200 OK";
    public static final String HTTP_INTERNAL_ERROR = "500 Internal Error";

    private static final String CRLF = "\r\n";

    private String status;

    private String version = "HTTP/1.0";

    private String body;

    private Map<String, String> headers;

    public String toString() {
        String response = "";
        response += version + " " + status + CRLF;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            response += entry.getKey() + ": " + entry.getValue() + CRLF;
        }
        if (!headers.containsKey("Content-Length")) {
            response += "Content-Length: " + body.length() + CRLF;
        }
        response += CRLF;
        response += body;
        return response;
    }

    public byte[] getBytes() {
        return toString().getBytes();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
