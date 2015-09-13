package com.github.olivereivak.p2md5.model;

public class HttpResponse {

    public static final String HTTP_OK = "200 OK";
    public static final String HTTP_INTERNAL_ERROR = "500 Internal Error";

    private static final String CRLF = "\r\n";

    private String status;

    private String server;

    private String contentType;

    private String body;

    public byte[] getBytes() {
        String response = "";
        response += "HTTP/1.0 " + status + " OK" + CRLF;
        response += "Server: " + server + CRLF;
        response += contentType + CRLF;
        response += "Content-Length: " + body.length() + CRLF;
        response += CRLF;
        response += body;

        return response.getBytes();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
