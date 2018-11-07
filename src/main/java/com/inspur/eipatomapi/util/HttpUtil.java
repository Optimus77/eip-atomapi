package com.inspur.eipatomapi.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * @author: jiasirui
 * @date: 2018/10/24 21:53
 * @description:
 */
public class HttpUtil {

    private final static Logger log = LoggerFactory.getLogger(HttpUtil.class);


    protected static HttpClient getCloseableHttpClient() throws Exception {
        try {
            return  HttpClients.createDefault();
        } catch (Exception e) {
            e.printStackTrace();
            throw  new Exception("et the clientBuilder from bean error");
        }

    }

    public static HttpResponse get(String url, Map<String,String > header){
        HttpGet httpGet = new HttpGet(url.toString());

        Iterator<Map.Entry<String, String>> it = header.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            httpGet.setHeader(entry.getKey(),entry.getValue());
        }
        try {
            HttpResponse httpResponse = getCloseableHttpClient().execute(httpGet);
            return httpResponse;
        } catch (Exception e) {
            log.error("http get error:"+e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static HttpResponse post(String url, Map<String,String > header, String body ) {
        HttpClient client;
        try {
            client = getCloseableHttpClient();
            HttpPost httpPost = new HttpPost(url.toString());
            Iterator<Map.Entry<String, String>> it = header.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                httpPost.setHeader(entry.getKey(),entry.getValue());
            }
            log.debug("request line:post-" + httpPost.getRequestLine());
            StringEntity entity = new StringEntity(body, HTTP.UTF_8);
//            entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
//            entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
            log.info("befor post: entity:{}", entity.toString());
            httpPost.setEntity(entity);
            HttpResponse httpResponse = client.execute(httpPost);
            return httpResponse;
        } catch (Exception e) {
            log.error("IO Exception when post.{}",e.getMessage());
            return null;
        }

    }



}
