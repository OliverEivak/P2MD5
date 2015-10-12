package com.github.olivereivak.p2md5.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.olivereivak.p2md5.App;
import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.HttpResponse;
import com.github.olivereivak.p2md5.model.Peer;
import com.github.olivereivak.p2md5.server.SimpleGet;
import com.github.olivereivak.p2md5.utils.FileUtils;

public class PeerService {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static List<Peer> getPeersFromFile(String path) {
        InputStream from = FileUtils.getFileAsStream(path);
        return getPeers(from);
    }

    public static List<Peer> getPeersFromURL(String url) {
        List<Peer> peers = new ArrayList<Peer>();
        HttpRequest request = new HttpRequest();
        URI uri;

        try {
            uri = new URI(url);
            request.setIp(InetAddress.getByName(uri.getHost()));
        } catch (UnknownHostException e) {
            logger.error("Failed to get InetAddress by name. ", e);
            return peers;
        } catch (URISyntaxException e) {
            logger.error("Failed to create URI object.", e);
            return peers;
        }

        request.setPort(80);
        request.setPath(uri.getPath());
        request.setMethod("GET");
        request.setVersion("1.0");
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "P2MD5/1.0");
        request.setHeaders(headers);

        logger.debug(request.toString());
        HttpResponse response = SimpleGet.get(request);
        logger.debug(response.toString());

        InputStream from = new ByteArrayInputStream(response.getBody().getBytes(StandardCharsets.UTF_8));
        return getPeers(from);
    }

    private static List<Peer> getPeers(InputStream from) {
        List<Peer> peers = new ArrayList<Peer>();
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<List<List<String>>> typeRef = new TypeReference<List<List<String>>>() {
        };

        List<List<String>> machines = new ArrayList<>();
        try {
            machines = mapper.readValue(from, typeRef);
        } catch (IOException e) {
            logger.error("Failed to map ip & port data. ", e);
        }

        for (List<String> machine : machines) {
            peers.add(new Peer(machine.get(0), Integer.valueOf(machine.get(1))));
            logger.debug("Adding peer {}:{}", machine.get(0), machine.get(1));
        }

        return peers;
    }

}
