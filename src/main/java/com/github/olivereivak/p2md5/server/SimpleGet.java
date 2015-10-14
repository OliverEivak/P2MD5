package com.github.olivereivak.p2md5.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.HttpResponse;

public class SimpleGet {

    private static Logger logger = LoggerFactory.getLogger(SimpleGet.class);

    private static final String CRLF = "\r\n";

    public static HttpResponse get(HttpRequest request) {
        logger.debug("Sending request {} {}", request.getMethod(), request.getPath());

        HttpResponse response = null;
        try {
            Socket socket = new Socket(request.getIp(), request.getPort());
            OutputStream output = socket.getOutputStream();
            output.write(request.getBytes());

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            response = parseResponse(br, socket);

            output.flush();
            output.close();
            socket.close();
        } catch (Exception e) {
            logger.error("GET request failed.", e);
        }

        return response;
    }

    private static HttpResponse parseResponse(BufferedReader br, Socket socket) throws Exception {
        HttpResponse response = new HttpResponse();

        String headerLine = br.readLine();
        StringTokenizer tokenizer = new StringTokenizer(headerLine);
        tokenizer.nextToken(); // ignore version
        response.setStatus(tokenizer.nextToken() + " " + tokenizer.nextToken());

        Map<String, String> headers = new HashMap<>();
        while (true) {
            headerLine = br.readLine();
            if (headerLine == null || headerLine.equals(CRLF) || headerLine.equals("")) {
                break;
            }

            String[] tokens = headerLine.split(":\\s", 2);
            if (tokens.length >= 2) {
                headers.put(tokens[0], tokens[1]);
            }
        }
        response.setHeaders(headers);

        int contentLength = headers.entrySet().stream().filter(header -> header.getKey().equals("Content-Length"))
                .mapToInt(entry -> Integer.valueOf(entry.getValue())).findFirst().orElse(0);
        int receivedLength = 0;

        StringBuilder sb = new StringBuilder();
        while (receivedLength < contentLength) {
            sb.append((char) br.read());
            receivedLength++;
        }
        response.setBody(sb.toString());

        return response;
    }

}
