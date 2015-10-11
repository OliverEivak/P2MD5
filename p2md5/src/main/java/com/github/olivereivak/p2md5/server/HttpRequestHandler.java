package com.github.olivereivak.p2md5.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.HttpResponse;

public class HttpRequestHandler implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    private static final String SERVER_NAME = "SimpleHttpServer";
    private static final String CRLF = "\r\n";

    private BlockingQueue<HttpRequest> requestQueue;

    private Socket socket;

    private OutputStream output;

    private BufferedReader br;

    public HttpRequestHandler(Socket socket, BlockingQueue<HttpRequest> requestQueue) throws Exception {
        this.socket = socket;
        this.output = socket.getOutputStream();
        this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.requestQueue = requestQueue;
    }

    public void run() {
        try {
            requestQueue.add(parseRequest());
            respondOK();

            br.close();
            output.close();
            socket.close();
        } catch (Exception e) {
            logger.error("Error handling request. ", e);
            try {
                respondError();
            } catch (IOException e1) {
                logger.error("Error sending error response. ", e);
            }
        }
    }

    private HttpRequest parseRequest() throws Exception {
        HttpRequest request = new HttpRequest();

        String headerLine = br.readLine();
        StringTokenizer tokenizer = new StringTokenizer(headerLine);
        request.setMethod(tokenizer.nextToken());
        request.setUri(tokenizer.nextToken());
        request.setVersion(tokenizer.nextToken());

        Map<String, String> headers = new HashMap<>();
        while (true) {
            headerLine = br.readLine();
            if (headerLine.equals(CRLF) || headerLine.equals("")) {
                break;
            }

            String[] tokens = headerLine.split(":\\s", 2);
            headers.put(tokens[0], tokens[1]);
        }
        request.setHeaders(headers);

        int contentLength = headers.entrySet().stream().filter(header -> header.getKey().equals("Content-Length"))
                .mapToInt(entry -> Integer.valueOf(entry.getValue())).findFirst().orElse(0);
        int receivedLength = 0;

        StringBuilder sb = new StringBuilder();
        while (receivedLength < contentLength) {
            sb.append((char) br.read());
            receivedLength++;
        }
        request.setBody(sb.toString());

        request.setIp(socket.getInetAddress());
        request.setPort(socket.getPort());

        return request;
    }

    private void respondOK() throws IOException {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpResponse.HTTP_OK);
        response.setServer(SERVER_NAME);
        response.setContentType("text/html");
        response.setBody("0");
        output.write(response.getBytes());
    }

    private void respondError() throws IOException {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpResponse.HTTP_INTERNAL_ERROR);
        response.setServer(SERVER_NAME);
        response.setContentType("text/html");
        response.setBody("<html><body><b>Internal Server Error</b></body></html>");
        output.write(response.getBytes());
    }

}
