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


//@Profile(value = "dev")
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class KeycloakClientRequestFactoryProd extends KeycloakClientRequestFactory implements ClientHttpRequestFactory {

    public KeycloakClientRequestFactoryProd() {
    }

    @Override
    protected void postProcessHttpRequest(HttpUriRequest request) {
        request.setHeader("dev_mode", "true");
    }


}
