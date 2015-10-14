package com.github.olivereivak.p2md5.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.apache.commons.collections4.map.MultiValueMap;
import org.easymock.EasyMockRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class HttpUtilsTest {

    @Test
    public void parseQueryParams() {
        MultiValueMap<String, String> queryParams = HttpUtils.parseQueryParams(
                "/resource?sendip=55.66.77.88&sendport=6788&ttl=5&id=wqeqwe23&noask=11.22.33.44_345&noask=111.222.333.444_223");

        assertEquals(Arrays.asList("55.66.77.88"), queryParams.getCollection("sendip"));
        assertEquals(Arrays.asList("6788"), queryParams.getCollection("sendport"));
        assertEquals(Arrays.asList("5"), queryParams.getCollection("ttl"));
        assertEquals(Arrays.asList("wqeqwe23"), queryParams.getCollection("id"));
        assertEquals(Arrays.asList("11.22.33.44_345", "111.222.333.444_223"), queryParams.getCollection("noask"));
    }

    @Test
    public void parseQueryParamsEmptyInput() {
        MultiValueMap<String, String> queryParams = HttpUtils.parseQueryParams("");
        assertNotNull(queryParams);
    }

    @Test
    public void parseQueryParamsNullInput() {
        MultiValueMap<String, String> queryParams = HttpUtils.parseQueryParams(null);
        assertNotNull(queryParams);
    }

    @Test
    public void parseQueryParamsOnlySlash() {
        MultiValueMap<String, String> queryParams = HttpUtils.parseQueryParams("/");
        assertNotNull(queryParams);
    }

    @Test
    public void queryParamsToString() {
        MultiValueMap<String, String> queryParams = new MultiValueMap<>();
        queryParams.put("key1", "value1");
        queryParams.put("key2", "test1");
        queryParams.put("key2", "test2");
        queryParams.put("key3", "value3");
        String queryString = HttpUtils.queryParamsToString(queryParams);
        assertEquals("key1=value1&key2=test1&key2=test2&key3=value3", queryString);
    }

}
