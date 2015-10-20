package com.github.olivereivak.p2md5.server;

import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMockRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.olivereivak.p2md5.model.HttpRequest;
import com.github.olivereivak.p2md5.model.HttpResponse;

@RunWith(EasyMockRunner.class)
public class SimpleGetTest {

    @Test
    public void get() throws UnknownHostException {
        HttpRequest request = new HttpRequest();
        request.setIp(InetAddress.getByName("dijkstra.cs.ttu.ee"));
        request.setPort(80);
        request.setPath("/~Oliver.Eivak/machines.txt");
        request.setMethod("GET");
        request.setVersion("HTTP/1.0");
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "P2MD5/1.0");
        request.setHeaders(headers);

        System.out.println(request.toString());
        System.out.println("=====");

        HttpResponse response = SimpleGet.get(request);

        assertNotNull(response);
        System.out.println(response.toString());
    }

}
