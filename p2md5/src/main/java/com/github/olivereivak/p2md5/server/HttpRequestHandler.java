package com.github.olivereivak.p2md5.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class HttpRequestHandler implements Runnable {

    final static String CRLF = "\r\n";

    Socket socket;

    OutputStream output;

    BufferedReader br;

    public HttpRequestHandler(Socket socket) throws Exception {
        this.socket = socket;
        this.output = socket.getOutputStream();
        this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void run() {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void processRequest() throws Exception {

        String headerLine = br.readLine();
        StringTokenizer tokenizer = new StringTokenizer(headerLine);
        String method = tokenizer.nextToken();
        String uri = tokenizer.nextToken();
        String version = tokenizer.nextToken();

        System.out.println("Method=" + method);
        System.out.println("Uri=" + uri);
        System.out.println("Version=" + version);

        Map<String, String> headers = new HashMap<>();
        while (true) {
            headerLine = br.readLine();
            if (headerLine.equals(CRLF) || headerLine.equals("")) {
                break;
            }

            String[] tokens = headerLine.split(":\\s", 2);
            headers.put(tokens[0], tokens[1]);
        }

        headers.keySet().stream().forEach(key -> System.out.println(key + "=" + headers.get(key)));

        String statusLine = "HTTP/1.0 200 OK" + CRLF;
        String serverLine = "Server: Simple Java Http Server" + CRLF;
        String contentTypeLine = "text/html" + CRLF;
        String entityBody = "<HTML>" + "<HEAD><TITLE>Test title</TITLE></HEAD>" + "<BODY>Hi" + "<br>This is text. "
                + "Wow.</BODY></HTML>";
        String contentLengthLine = "Content-Length: " + entityBody.length() + CRLF;

        output.write(statusLine.getBytes());
        output.write(serverLine.getBytes());
        output.write(contentTypeLine.getBytes());
        output.write(contentLengthLine.getBytes());

        output.write(CRLF.getBytes());

        output.write(entityBody.getBytes());

        output.close();
        br.close();
        socket.close();

    }

}
