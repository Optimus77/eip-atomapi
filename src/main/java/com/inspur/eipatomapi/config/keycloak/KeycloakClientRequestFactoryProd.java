package com.inspur.eipatomapi.config.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Profile(value = "prod")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class KeycloakClientRequestFactoryProd extends KeycloakClientRequestFactory implements ClientHttpRequestFactory {

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    public final static Logger log = LoggerFactory.getLogger(KeycloakClientRequestFactoryProd.class);
    @Override
    protected void postProcessHttpRequest(HttpUriRequest request) {
        KeycloakSecurityContext context = this.getKeycloakSecurityContext();
        String tokenString = null;
        if (context != null) {
            tokenString = context.getTokenString();
        } else {
            Map<String, String> params = new HashMap<String, String>();
            params.put("grant_type", "client_credentials");
            params.put("client_id", clientId);
            params.put("client_secret", clientSecret);
            Map res = httpPostForm(getTokenUrl(), params, null, "utf-8");
            if ((Integer) res.get("statusCode") == 200) {
                String body = (String) res.get("body");
                {
                    ObjectMapper obj = new ObjectMapper();
                    try {
                        Map map = obj.readValue(body, Map.class);
                        tokenString = map.get("access_token").toString();
                        System.out.println(tokenString);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        request.setHeader(AUTHORIZATION_HEADER, "Bearer " + tokenString);
    }

    private String getTokenUrl() {
        String url = "";
        if (!authServerUrl.endsWith("/")) {
            authServerUrl = authServerUrl + "/";
        }
        url = authServerUrl + "realms/" + realm + "/protocol/openid-connect/token";
        return url;
    }

    @Override
    protected KeycloakSecurityContext getKeycloakSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        KeycloakAuthenticationToken token;
        KeycloakSecurityContext context;
        if (authentication == null) {
            return null;
        }
        if (!KeycloakAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            throw new IllegalStateException(
                    String.format(
                            "Cannot set authorization header because Authentication is of type %s but %s is required",
                            authentication.getClass(), KeycloakAuthenticationToken.class)
            );
        }

        token = (KeycloakAuthenticationToken) authentication;
        context = token.getAccount().getKeycloakSecurityContext();

        return context;
    }

    public KeycloakClientRequestFactoryProd() {
        //创建自定义的httpclient对象  
        CloseableHttpClient client = HttpClients.custom().setConnectionManager(createConnManager()).build();
        super.setHttpClient(client);
    }

    private static PoolingHttpClientConnectionManager createConnManager() {
        //采用绕过验证的方式处理https请求
        SSLContext sslcontext = null;
        try {
            sslcontext = createIgnoreVerifySSL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 设置协议http和https对应的处理socket链接工厂的对象
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", new SSLConnectionSocketFactory(sslcontext))
                .build();
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        return connManager;
    }

    public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSLv3");  //TLS
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) {
                log.info("Todo, add checkClientTrusted.");
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) {
                log.info("Todo: add checkServerTrusted");
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        sc.init(null, new X509TrustManager[]{trustManager}, null);
        return sc;
    }

    public static Map httpPostForm(String url, Map<String, String> params, Map<String, String> headers, String encode) {
        Map response = new HashMap();
        if (encode == null) {
            encode = "utf-8";
        }
        CloseableHttpClient closeableHttpClient = HttpClients.custom().setConnectionManager(createConnManager()).build();
        HttpPost httpost = new HttpPost(url);

        //设置header  
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpost.setHeader(entry.getKey(), entry.getValue());
            }
        }
        //组织请求参数    
        List<NameValuePair> paramList = new ArrayList<NameValuePair>();
        if (params != null && params.size() > 0) {
            Set<String> keySet = params.keySet();
            for (String key : keySet) {
                paramList.add(new BasicNameValuePair(key, params.get(key)));
            }
        }
        try {
            httpost.setEntity(new UrlEncodedFormEntity(paramList, encode));
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String content = null;
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = closeableHttpClient.execute(httpost);
            HttpEntity entity = httpResponse.getEntity();
            content = EntityUtils.toString(entity, encode);
            response.put("body", content);
            response.put("headers", httpResponse.getAllHeaders());
            response.put("statusCode", httpResponse.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(httpResponse!=null){
                    httpResponse.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {  //关闭连接、释放资源    
            closeableHttpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}
