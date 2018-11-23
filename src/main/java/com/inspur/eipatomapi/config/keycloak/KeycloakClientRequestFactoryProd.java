package com.inspur.eipatomapi.config.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspur.eipatomapi.util.HttpsClientUtil;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

//@Profile(value = "prod")
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


    @Override
    protected void postProcessHttpRequest(HttpUriRequest request) {
        KeycloakSecurityContext context = this.getKeycloakSecurityContext();
        String tokenString = null;
        if (context != null) {
            tokenString = context.getTokenString();
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "client_credentials");//password
            params.put("client_id", clientId);
            params.put("client_secret", clientSecret);
            Map res = httpPostForm(getTokenUrl(), params, null, "utf-8");
            if ((Integer) res.get("statusCode") == 200) {
                String body = (String) res.get("body");
                ObjectMapper obj = new ObjectMapper();
                try {
                    Map map = obj.readValue(body, Map.class);
                    tokenString = map.get("access_token").toString();
                    /**System.out.println(tokenString);*/
                } catch (IOException e) {

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
        //Create custom-made httpclient object
        CloseableHttpClient client = HttpsClientUtil.getHttpsClient();
        super.setHttpClient(client);
    }

    public static Map httpPostForm(String url, Map<String, String> params, Map<String, String> headers, String encode) {
        Map response = new HashMap();
        if (encode == null) {
            encode = "utf-8";
        }
        CloseableHttpClient closeableHttpClient = HttpsClientUtil.getHttpsClient();
        HttpPost httpost = new HttpPost(url);

        //set header
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpost.setHeader(entry.getKey(), entry.getValue());
            }
        }
        //Organization request parameter
        List<NameValuePair> paramList = new ArrayList<>();
        if (params != null && params.size() > 0) {
            Set<String> keySet = params.keySet();
            for (String key : keySet) {
                paramList.add(new BasicNameValuePair(key, params.get(key)));
            }
        }
        try {
            httpost.setEntity(new UrlEncodedFormEntity(paramList, encode));
        } catch (UnsupportedEncodingException e1) {

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
            //ResponseEntityUtil.getExceptionLog(e);
        } finally {
            try {
                if (null != httpResponse) {
                    httpResponse.close();
                }
            } catch (IOException e) {
                //ResponseEntityUtil.getExceptionLog(e);
            }
        }
        try {  //Close connections, free resources
            closeableHttpClient.close();
        } catch (IOException e) {
            //throw new MyException(e.getMessage());
        }
        return response;
    }
}
