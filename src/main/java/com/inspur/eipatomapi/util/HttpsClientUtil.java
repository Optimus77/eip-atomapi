package com.inspur.eipatomapi.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;


/**
 * @ClassName HttpsClientUtil
 * @Description Handling of HTTPs and HTTP
 * @Author zhang_xiaoyu
 * @CreateDate 2018/9/13 18:33:00
 * @Version ECS-3.0.0
 * @Reviewed :
 * @UpateLog :
 * Name                Date              Reason/Contents
 * ------------------------------------------------------------
 * ************  2018-**-** **:**:**    *********************
 */
@Slf4j
@Data
public class HttpsClientUtil {

    static boolean ignoreSSL = Boolean.TRUE;

    private HttpsClientUtil() {
    }


    /**
     * @return : org.apache.http.impl.client.CloseableHttpClient
     * @Description : get HttpsClient
     * @Author : zhang_xiaoyu
     * @CreateDate : 2018/9/13 19:09:30
     */
    public static CloseableHttpClient getHttpsClient() {

        CloseableHttpClient httpClient;
        if (ignoreSSL) {
            SSLContext sslContext = null;
            try {
                sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
                    @Override
                    public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                        return true;
                    }
                }).build();
            } catch (NoSuchAlgorithmException e) {

            } catch (KeyManagementException e) {

            } catch (KeyStoreException e) {

            }
            httpClient = HttpClients.custom().setSSLContext(sslContext).
                    setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
        } else {
            httpClient = HttpClients.createDefault();
        }
        return httpClient;
    }

    /**
     * @return : org.springframework.web.client.RestTemplate
     * @Description : Get build RestTemplate
     * @Author : zhang_xiaoyu
     * @CreateDate : 2018/9/13 19:09:30
     */
    public static RestTemplate buildRestTemplate() {

        RestTemplate restTemplate = new RestTemplate();
        if (ignoreSSL) {
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
            factory.setConnectionRequestTimeout(300000);
            factory.setConnectTimeout(300000);
            factory.setReadTimeout(300000);
            // https
            CloseableHttpClient httpClient = getHttpsClient();
            factory.setHttpClient(httpClient);
            restTemplate = new RestTemplate(factory);
        }
        reInitMessageConverter(restTemplate);
        return restTemplate;
    }

    /**
     * @param restTemplate : RestTemplate instance
     * @Description : Resolving RestTemplate garbled
     * @Author : zhang_xiaoyu
     * @CreateDate : 2018/9/27 20:15:30
     */
    private static void reInitMessageConverter(RestTemplate restTemplate) {

        List<HttpMessageConverter<?>> converterList = restTemplate.getMessageConverters();
        HttpMessageConverter<?> converterTarget = null;
        for (HttpMessageConverter<?> item : converterList) {
            if (item.getClass() == StringHttpMessageConverter.class) {
                converterTarget = item;
                break;
            }
        }
        if (converterTarget != null) {
            converterList.remove(converterTarget);
        }
        HttpMessageConverter<?> converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converterList.add(1, converter);
    }
}
