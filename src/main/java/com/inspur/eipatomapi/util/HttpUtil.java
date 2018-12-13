package com.inspur.eipatomapi.util;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class HttpUtil {

    private final static Logger log = LoggerFactory.getLogger(HttpUtil.class);


    private static HttpClient getCloseableHttpClient() throws Exception {
        try {
            return  HttpClients.createDefault();
        } catch (Exception e) {
            e.printStackTrace();
            throw  new Exception("et the clientBuilder from bean error");
        }

    }

    public static String get(String url, Map<String,String > header) throws  Exception{
        HttpGet httpGet = new HttpGet(url);

        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(1000)
                .setSocketTimeout(1000).setConnectTimeout(1000).build();
        httpGet.setConfig(requestConfig);
        if(null != header) {
            Iterator<Map.Entry<String, String>> it = header.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                httpGet.setHeader(entry.getKey(), entry.getValue());
            }
        }
        try {
            HttpResponse httpResponse = getCloseableHttpClient().execute(httpGet);
            String resultString = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
            return resultString;
        } catch (Exception e) {
            log.error("http get error:"+e.getMessage());
        }
        throw new EipException("Get request use http error.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    public static HttpResponse post(String url, Map<String,String > header, String body ) {
        HttpClient client;
        try {
            client = getCloseableHttpClient();
            HttpPost httpPost = new HttpPost(url);
            Iterator<Map.Entry<String, String>> it = header.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                httpPost.setHeader(entry.getKey(),entry.getValue());
            }
            log.debug("request line:post-" + httpPost.getRequestLine());
            StringEntity entity = new StringEntity(body, HTTP.UTF_8);
            log.debug("befor post: entity:{}", entity.toString());
            httpPost.setEntity(entity);
            HttpResponse httpResponse = client.execute(httpPost);
            return httpResponse;
        } catch (Exception e) {
            log.error("IO Exception when post.{}",e);
            return null;
        }

    }



}
