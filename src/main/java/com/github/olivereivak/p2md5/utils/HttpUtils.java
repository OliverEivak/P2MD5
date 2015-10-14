package com.github.olivereivak.p2md5.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.apache.commons.collections4.map.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {

    private static Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static MultiValueMap<String, String> parseQueryParams(String uri) {
        MultiValueMap<String, String> queryParams = new MultiValueMap<>();
        if (uri == null) {
            return queryParams;
        }

        String[] buffer = uri.split("\\?", 2);

        if (buffer.length == 2) {
            String[] keyValuePairs = buffer[1].split("&");

            for (String keyValuePair : keyValuePairs) {
                String tokens[] = keyValuePair.split("=", 2);
                if (tokens.length == 2) {
                    queryParams.put(tokens[0], tokens[1]);
                } else if (tokens.length == 1) {
                    queryParams.put(tokens[0], "");
                }
            }
        }

        return queryParams;
    }

    public static String queryParamsToString(MultiValueMap<String, String> queryParams) {
        String queryString = "";

        for (String key : queryParams.keySet()) {
            Collection<String> values = queryParams.getCollection(key);
            for (String value : values) {
                queryString += key + "=" + value + "&";
            }
        }

        return queryString.substring(0, queryString.length() - 1);
    }

    public static String getIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("Error getting ip. ", e);
            return "";
        }
    }

}
