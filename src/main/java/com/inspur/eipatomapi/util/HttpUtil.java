package com.inspur.eipatomapi.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class HttpUtil {

    private static ConnectionManager connectionFactory = new ConnectionManager();


    public static HttpResponse doGet(String url, Map<String, String> param, Map<String, String> headParam) throws IOException, URISyntaxException {

        // get Httpclient object
        CloseableHttpClient httpClient = connectionFactory.getHttpClient();
        CloseableHttpResponse response = null;
        HttpResponse httpResponse = null;
        try {
            // create uri
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                param.keySet().forEach(aParam -> builder.addParameter(aParam, param.get(aParam)));
            }
            URI uri = builder.build();
            // create http GET request
            HttpGet httpGet = new HttpGet(uri);
            if (headParam != null) {
                headParam.keySet().forEach(aParam -> httpGet.addHeader(aParam, headParam.get(aParam)));
            }
            // execute request
            response = httpClient.execute(httpGet);
            httpResponse = HttpResponse
                    .builder()
                    .responseBody(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                    .statusCode(response.getStatusLine().getStatusCode())
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return httpResponse;
    }

    public static HttpResponse doGetWithHeaders(String url, Map<String, String> param, HttpHeaders httpHeaders) throws IOException, URISyntaxException {

        // get Httpclient object
        CloseableHttpClient httpClient = connectionFactory.getHttpClient();
        CloseableHttpResponse response = null;
        HttpResponse httpResponse = null;
        try {
            // create uri
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                param.keySet().forEach(aParam -> builder.addParameter(aParam, param.get(aParam)));
            }
            URI uri = builder.build();
            // create http GET request
            HttpGet httpGet = new HttpGet(uri);
            if (httpHeaders != null) {
                httpHeaders.keySet().forEach(key -> httpGet.addHeader(key, httpHeaders.get(key).get(0)));
            }
            // execute request
            response = httpClient.execute(httpGet);
            httpResponse = HttpResponse
                    .builder()
                    .responseBody(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                    .statusCode(response.getStatusLine().getStatusCode())
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return httpResponse;
    }

    public static HttpResponse doGet(String url, Map<String, String> param) throws IOException, URISyntaxException {
        return doGet(url, param, null);
    }

    public static HttpResponse doGet(String url) throws IOException, URISyntaxException {
        return doGet(url, null, null);
    }

    public static HttpResponse doPost(String url, Map<String, String> param, Map<String, String> headParam) throws IOException {
        // get Httpclient object
        CloseableHttpClient httpClient = connectionFactory.getHttpClient();
        CloseableHttpResponse response = null;
        HttpResponse httpResponse = null;
        try {
            // create Http Post request
            HttpPost httpPost = new HttpPost(url);
            if (param != null) {
                List<NameValuePair> paramList = new ArrayList<>();
                param.keySet().forEach(aParam -> paramList.add(new BasicNameValuePair(aParam, param.get(aParam))));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList);
                httpPost.setEntity(entity);
            }
            //add headers
            if (headParam != null) {
                headParam.keySet().forEach(aParam -> httpPost.addHeader(aParam, headParam.get(aParam)));
            }
            // execute http request
            response = httpClient.execute(httpPost);
            httpResponse = HttpResponse
                    .builder()
                    .responseBody(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                    .statusCode(response.getStatusLine().getStatusCode())
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return httpResponse;
    }

    public static HttpResponse doPost(String url, Map<String, String> param) throws IOException {
        return doPost(url, param, null);
    }

    public static HttpResponse doPostJson(String url, String json, Map<String, String> headParam) throws IOException {
        // get Httpclient object
        CloseableHttpClient httpClient = connectionFactory.getHttpClient();
        CloseableHttpResponse response = null;
        HttpResponse httpResponse = null;
        try {
            // create Http Post request
            HttpPost httpPost = new HttpPost(url);
            // add request body
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            //add headers
            if (headParam != null) {
                headParam.keySet().forEach(aParam -> httpPost.addHeader(aParam, headParam.get(aParam)));
            }
            // execute http request
            response = httpClient.execute(httpPost);
            // assemble return result
            httpResponse = HttpResponse
                    .builder()
                    .responseBody(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                    .statusCode(response.getStatusLine().getStatusCode())
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return httpResponse;
    }

    public static HttpResponse doPostJson(String url, String json) throws IOException {
        return doPostJson(url, json, null);
    }

    public static HttpResponse doPut(String url, Map<String, String> param, Map<String, String> headParam) throws IOException {
        // get Httpclient object
        CloseableHttpClient httpClient = connectionFactory.getHttpClient();
        CloseableHttpResponse response = null;
        HttpResponse httpResponse = null;
        try {
            // create Http Put request
            HttpPut httpPut = new HttpPut(url);
            if (param != null) {
                List<NameValuePair> paramList = new ArrayList<>();
                param.keySet().forEach(aParam -> paramList.add(new BasicNameValuePair(aParam, param.get(aParam))));
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList);
                httpPut.setEntity(entity);
            }
            //add headers
            if (headParam != null) {
                headParam.keySet().forEach(aParam -> httpPut.addHeader(aParam, headParam.get(aParam)));
            }
            // execute http request
            response = httpClient.execute(httpPut);
            httpResponse = HttpResponse
                    .builder()
                    .responseBody(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                    .statusCode(response.getStatusLine().getStatusCode())
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return httpResponse;
    }

    public static HttpResponse doPut(String url, Map<String, String> param) throws IOException {
        return doPut(url, param, null);
    }

    public static HttpResponse doPutJson(String url, String json, Map<String, String> headParam) throws IOException {
        // get Httpclient object
        CloseableHttpClient httpClient = connectionFactory.getHttpClient();
        CloseableHttpResponse response = null;
        HttpResponse httpResponse = null;
        try {
            // create Http Put request
            HttpPut httpPut = new HttpPut(url);
            // add request body
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPut.setEntity(entity);
            //add headers
            if (headParam != null) {
                headParam.keySet().forEach(aParam -> httpPut.addHeader(aParam, headParam.get(aParam)));
            }
            // execute http request
            response = httpClient.execute(httpPut);
            httpResponse = HttpResponse
                    .builder()
                    .responseBody(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                    .statusCode(response.getStatusLine().getStatusCode())
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return httpResponse;
    }

    public static HttpResponse doPutJson(String url, String json) throws IOException {
        return doPutJson(url, json, null);
    }


    public static HttpResponse doDelete(String url, Map<String, String> param, Map<String, String> headParam) throws IOException, URISyntaxException {

        // get Httpclient object
        CloseableHttpClient httpClient = connectionFactory.getHttpClient();
        CloseableHttpResponse response = null;
        HttpResponse httpResponse = null;
        try {
            // create uri
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                param.keySet().forEach(aParam -> builder.addParameter(aParam, param.get(aParam)));
            }
            URI uri = builder.build();
            // create http Delete request
            HttpDelete httpDelete = new HttpDelete(uri);
            //add headers
            if (headParam != null) {
                headParam.keySet().forEach(aParam -> httpDelete.addHeader(aParam, headParam.get(aParam)));
            }
            // execute request
            response = httpClient.execute(httpDelete);

            httpResponse = HttpResponse
                    .builder()
                    .responseBody(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8))
                    .statusCode(response.getStatusLine().getStatusCode())
                    .build();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return httpResponse;
    }

    public static HttpResponse doDelete(String url, Map<String, String> param) throws IOException, URISyntaxException {
        return doDelete(url, param, null);
    }


    /**
     * 使用连接池来创建CloseableHttpClient
     */
    public static class ConnectionManager {

        //最大连接数
        private static final int MAX_TOTAL = 10000;
        // 每一个路由的最大连接数
        private static final int MAX_PER_ROUTE = 500;
        //从连接池中获得连接的超时时间
        private static final int CONNECTION_REQUEST_TIMEOUT = 60000;
        //连接超时
        private static final int CONNECTION_TIMEOUT = 60000;
        //获取数据的超时时间
        private static final int SOCKET_TIMEOUT = 60000;

        PoolingHttpClientConnectionManager cm;
        CloseableHttpClient httpClient;

        /**
         * 重连接策略
         */
        HttpRequestRetryHandler retryHandler = (exception, executionCount, context) -> {
            // Do not retry if over max retry count
            if (executionCount >= 3) {
                log.error("[HttpClientUtil] : Over max retry count!", exception);
                return false;
            }
            // Timeout
            if (exception instanceof InterruptedIOException) {
                log.error("[HttpClientUtil] : Timeout!", exception);
                return false;
            }
            // Unknown host
            if (exception instanceof UnknownHostException) {
                log.error("[HttpClientUtil] : Unknown host!", exception);
                return false;
            }
            // Connection refused
            if (exception instanceof ConnectTimeoutException) {
                log.error("[HttpClientUtil] : Connection refused!", exception);
                return false;
            }
            // SSL handshake exception
            if (exception instanceof SSLException) {
                log.error("[HttpClientUtil] : SSL handshake exception!", exception);
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();

            // Retry if the request is considered idempotent
            return !(request instanceof HttpEntityEnclosingRequest);
        };


        /**
         * 配置连接参数
         */
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build();

        public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
            //TLS
            SSLContext sc = SSLContext.getInstance("SSLv3");
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) {
                    //I don't know why this method is empty
                }

                @Override
                public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString) {
                    //I don't know why this method is empty
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            sc.init(null, new X509TrustManager[]{trustManager}, null);
            return sc;
        }

        ConnectionManager() {
            SSLContext sslContext = null;
            try {
                sslContext = createIgnoreVerifySSL();
            } catch (NoSuchAlgorithmException e) {
                log.error("[HttpClientUtil] : NoSuchAlgorithmException!", e);
            } catch (KeyManagementException e) {
                log.error("[HttpClientUtil] : KeyManagementException!", e);
            }
            Registry<ConnectionSocketFactory> socketFactoryRegistry;
            if (sslContext != null) {
                socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                        .register("https", new SSLConnectionSocketFactory(sslContext))
                        .build();
            } else {
                socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                        .build();
            }

            cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            cm.setMaxTotal(MAX_TOTAL);
            cm.setDefaultMaxPerRoute(MAX_PER_ROUTE);


            // 定制实现HttpClient，全局只有一个HttpClient
            httpClient = HttpClients.custom()
                    .setConnectionManager(cm)
                    .setDefaultRequestConfig(requestConfig)
                    .setRetryHandler(retryHandler)
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();

        }

        CloseableHttpClient getHttpClient() {
            return httpClient;
        }

        public HttpClientConnectionManager getManager() {
            return cm;
        }
    }

}