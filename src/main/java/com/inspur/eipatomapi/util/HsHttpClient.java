package com.inspur.eipatomapi.util;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;

import com.inspur.eipatomapi.entity.Cookie;
import com.inspur.eipatomapi.entity.FwLogin;
import com.inspur.eipatomapi.entity.FwLoginResponseBody;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class HsHttpClient {
    private static final Logger logger = Logger.getLogger(HsHttpClient.class);
    private static Map<String, String> cookieMap = new HashMap();

    public HsHttpClient() {
    }

    private static String getCookie(String manageIp) {
        return cookieMap.get(manageIp);
    }

    private static boolean isHaveCookie(String manageIp) {
        return cookieMap.containsKey(manageIp);
    }

    private static void putCookie(String manageIp, String cookie) {
        cookieMap.put(manageIp, cookie);
    }

    private static byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        boolean var3 = true;

        int len;
        while((len = inStream.read(buffer)) != -1) {
            outSteam.write(buffer, 0, len);
        }

        outSteam.close();
        inStream.close();
        return outSteam.toByteArray();
    }

    private static String getResponseString(HttpResponse response) throws UnsupportedOperationException, IOException, JSONException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream instream = entity.getContent();

            String var6;
            try {
                Object var3 = null;

                byte[] payload;
                try {
                    payload = readStream(instream);
                } catch (Exception var9) {
                    logger.error(var9);
                    return "";
                }

                if (payload.length == 0) {
                    var6 = getJson(response.getStatusLine().getStatusCode());
                    return var6;
                }

                JSONObject jo = new JSONObject(new String(payload));
                var6 = jo.toString();
            } finally {
                instream.close();
            }

            return var6;
        } else {
            return getJson(response.getStatusLine().getStatusCode());
        }
    }

    private static String getJson(int code) {
        return code != 200 && code != 204 ? "{\"success\":false, \"result\":[], \"exception\":{}}" : "{\"success\":true, \"result\":[], \"exception\":{}}";
    }

    private static boolean isLogin(String ip, String port) {
        logger.info("判断登录防火墙状态:" + ip + port);
        if (!isHaveCookie(ip)) {
            logger.info("无防火墙cookie！");
            return false;
        } else {
            StringBuffer url = new StringBuffer();
            url.append("https://").append(ip);
            if (port != null && !"".equals(port)) {
                url.append(":" + port);
            }

            url.append("/rest/login");
            CloseableHttpClient client = getHttpsClient();
            HttpGet httpGet = new HttpGet(url.toString());
            httpGet.setHeader("Content-Type", "application/json");
            httpGet.setHeader("Cookie", getCookie(ip));
            logger.debug("request line:" + httpGet.getRequestLine());
            FwLoginResponseBody body = new FwLoginResponseBody();

            try {
                new Gson();
                String strlogin = EntityUtils.toString(client.execute(httpGet).getEntity());
                boolean success = true;
                if (!strlogin.contains("\"success\":true") && !strlogin.contains("\"success\" : true")) {
                    success = false;
                } else {
                    success = true;
                }

                body.setSuccess(success);
                if (body.isSuccess()) {
                    logger.info("已登录防火墙！");
                } else {
                    logger.info("未登录防火墙!");
                }

                boolean var10 = body.isSuccess();
                return var10;
            } catch (ClientProtocolException var20) {
                logger.error(var20);
                return false;
            } catch (IOException var21) {
                logger.error(var21);
            } finally {
                try {
                    if (client != null) {
                        client.close();
                    }
                } catch (IOException var19) {
                    logger.error(var19);
                }

            }

            return false;
        }
    }

    private static String loginCookieParser(JSONObject jo) throws Exception {
        boolean succflag = jo.getBoolean("success");
        if (succflag) {
            JSONObject resultJsn = jo.getJSONObject("result");
            String token = resultJsn.getString("token");
            String platform = resultJsn.getString("platform");
            String hw_platform = resultJsn.getString("hw_platform");
            String host_name = resultJsn.getString("host_name");
            String company = resultJsn.getString("company");
            String oemid = resultJsn.getString("oemId");
            String vsysid = resultJsn.getString("vsysId");
            String vsysname = resultJsn.getString("vsysName");
            String role = resultJsn.getString("role");
            String license = resultJsn.getString("license");
            String httpProtocol = resultJsn.getString("httpProtocol");
            JSONObject sysInfoObj = resultJsn.getJSONObject("sysInfo");
            String soft_version = sysInfoObj.getString("soft_version");
            String username = "InnetAdmin";
            String overseaLicense = resultJsn.getString("overseaLicense");
            String HS_frame_lang = "zh_CN";
            Cookie cookie = new Cookie(token, platform, hw_platform, host_name, company, oemid, vsysid, vsysname, role, license, httpProtocol, soft_version, username, overseaLicense, HS_frame_lang);
            System.out.println(cookie.toString());
            String rstCookie = "fromrootvsys=true;" + cookie.toString();
            return rstCookie;
        } else {
            logger.error("no found result:" + jo);
            return "";
        }
    }

    private static CloseableHttpClient getHttpsClient() {
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        ConnectionSocketFactory plainSF = new PlainConnectionSocketFactory();
        registryBuilder.register("http", plainSF);

        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            TrustStrategy anyTrustStrategy = new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            };
            SSLContext sslContext = SSLContexts.custom().useTLS().loadTrustMaterial(trustStore, anyTrustStrategy).build();
            LayeredConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            registryBuilder.register("https", sslSF);
        } catch (KeyStoreException var6) {
            throw new RuntimeException(var6);
        } catch (KeyManagementException var7) {
            throw new RuntimeException(var7);
        } catch (NoSuchAlgorithmException var8) {
            throw new RuntimeException(var8);
        }

        Registry<ConnectionSocketFactory> registry = registryBuilder.build();
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);
        return HttpClientBuilder.create().setConnectionManager(connManager).build();
    }

    private static boolean httpLogin(String url, String ip, String json) throws Exception {
        CloseableHttpClient httpclient = getHttpsClient();
        logger.debug("httpLogin内部：开始登陆,URL:" + url + " IP:" + ip + " json:" + json);
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json");
        StringEntity se = new StringEntity(json, "UTF-8");
        se.setContentType("text/json");
        se.setContentEncoding(new BasicHeader("Content-Type", "application/json"));
        httpPost.setEntity(se);
        CloseableHttpResponse response = httpclient.execute(httpPost);
        InputStream instream = null;

        label135: {
            try {
                HttpEntity entity = response.getEntity();
                logger.debug("HttpEntity:" + entity.toString());
                if (entity == null) {
                    break label135;
                }

                instream = entity.getContent();
                byte[] payload = readStream(instream);
                JSONObject jo = new JSONObject(new String(payload));
                String loginResult = loginCookieParser(jo);
                if (loginResult == null || loginResult == "") {
                    logger.debug("httpLogin内部：登陆失败");
                    return false;
                }

                logger.debug("httpLogin内部： COOKIE信息  IP:" + ip + " loginResult:" + loginResult);
                putCookie(ip, loginResult);
                logger.debug("httpLogin内部：登陆成功");
            } catch (IOException var15) {
                logger.error(var15);
                break label135;
            } finally {
                if (instream != null) {
                    instream.close();
                }

                if (response != null) {
                    response.close();
                }

            }

            return true;
        }

        logger.debug("httpLogin内部：登陆失败");
        return false;
    }

    private static boolean login(String ip, String port, String login, int tryTimes) throws Exception {
        logger.info("登录防火墙:" + ip + port + login);
        StringBuffer url = new StringBuffer();
        url.append("https://").append(ip);
        if (port != null && !"".equals(port)) {
            url.append(":" + port);
        }

        url.append("/rest/login");
        boolean flag = true;

        try {
            ++tryTimes;
            logger.info("登录防火墙参数: url:" + url.toString() + "ip:" + ip + "login:" + login);
            flag = httpLogin(url.toString(), ip, login);
            if (flag) {
                logger.info("登录防火墙: 成功!");
            } else {
                logger.info("登录防火墙: 失败!");
            }
        } catch (Exception var9) {
            var9.printStackTrace();
        }

        if (!flag) {
            Thread.sleep(1000L);
            return tryTimes < 3 ? login(ip, port, login, tryTimes) : false;
        } else {
            return true;
        }
    }

    public static String hsHttpGet(String ip, String port, String user, String pwd, String rest) throws Exception {
        Gson gson = new Gson();
        if (!isLogin(ip, port)) {
            FwLogin login = new FwLogin();
            if (user != null && !"".equals(user)) {
                login.setUserName(user);
                login.setPassword(pwd);
            }

            String loginUrl = gson.toJson(login);
            if (!login(ip, port, loginUrl, 0)) {
                return "";
            }
        }

        StringBuffer url = new StringBuffer();
        url.append("https://").append(ip);
        if (port != null && !"".equals(port)) {
            url.append(":" + port);
        }

        url.append(rest);
        CloseableHttpClient client = getHttpsClient();
        HttpGet httpGet = new HttpGet(url.toString());
        httpGet.setHeader("Content-Type", "application/json");
        httpGet.setHeader("Hillstone-language", "zh_CN");
        httpGet.setHeader("Cookie", getCookie(ip));
        logger.debug("request line:get-" + httpGet.getRequestLine());

        try {
            HttpResponse httpResponse = client.execute(httpGet);
            String var11 = getResponseString(httpResponse);
            return var11;
        } catch (IOException var19) {
            System.out.println(var19);
            logger.debug(var19);
        } finally {
            try {
                client.close();
            } catch (IOException var18) {
                logger.debug(var18);
            }

        }

        return "";
    }

    public static String hsHttpPost(String ip, String port, String user, String pwd, String rest, String payload) throws Exception {
        Gson gson = new Gson();
        if (!isLogin(ip, port)) {
            FwLogin login = new FwLogin();
            if (user != null && !"".equals(user)) {
                login.setUserName(user);
                login.setPassword(pwd);
            }

            String loginUrl = gson.toJson(login);
            if (!login(ip, port, loginUrl, 0)) {
                return "";
            }
        }

        StringBuffer url = new StringBuffer();
        url.append("https://").append(ip);
        if (port != null && !"".equals(port)) {
            url.append(":" + port);
        }

        url.append(rest);
        CloseableHttpClient client = getHttpsClient();
        HttpPost httpPost = new HttpPost(url.toString());
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Cookie", getCookie(ip));
        logger.debug("request line:post-" + httpPost.getRequestLine());

        try {
            StringEntity entity = new StringEntity(payload, "UTF-8");
            entity.setContentType("text/json");
            entity.setContentEncoding(new BasicHeader("Content-Type", "application/json"));
            httpPost.setEntity(entity);
            HttpResponse httpResponse = client.execute(httpPost);
            String var13 = getResponseString(httpResponse);
            return var13;
        } catch (IOException var21) {
            logger.error(var21);
        } finally {
            try {
                client.close();
            } catch (IOException var20) {
                logger.error(var20);
            }

        }

        return "";
    }

    public static String hsHttpPut(String ip, String port, String user, String pwd, String rest, String payload) throws Exception {
        Gson gson = new Gson();
        if (!isLogin(ip, port)) {
            FwLogin login = new FwLogin();
            if (user != null && !"".equals(user)) {
                login.setUserName(user);
                login.setPassword(pwd);
            }

            String loginUrl = gson.toJson(login);
            if (!login(ip, port, loginUrl, 0)) {
                return "";
            }
        }

        StringBuffer url = new StringBuffer();
        url.append("https://").append(ip);
        if (port != null && !"".equals(port)) {
            url.append(":" + port);
        }

        url.append(rest);
        CloseableHttpClient client = getHttpsClient();
        HttpPut httpPut = new HttpPut(url.toString());
        httpPut.setHeader("Content-Type", "application/json");
        httpPut.setHeader("Cookie", getCookie(ip));
        logger.debug("request line:put-" + httpPut.getRequestLine());

        try {
            StringEntity entity = new StringEntity(payload, "UTF-8");
            entity.setContentType("text/json");
            entity.setContentEncoding(new BasicHeader("Content-Type", "application/json"));
            httpPut.setEntity(entity);
            HttpResponse httpResponse = client.execute(httpPut);
            String var13 = getResponseString(httpResponse);
            return var13;
        } catch (IOException var21) {
            ;
        } finally {
            try {
                client.close();
            } catch (IOException var20) {
                logger.error(var20);
            }

        }

        return "";
    }


    public static String hsHttpDelete(String ip, String port, String user, String pwd, String rest, String payload) throws Exception {
        Gson gson = new Gson();
        if (!isLogin(ip, port)) {
            FwLogin login = new FwLogin();
            if (user != null && !"".equals(user)) {
                login.setUserName(user);
                login.setPassword(pwd);
            }

            String loginUrl = gson.toJson(login);
            if (!login(ip, port, loginUrl, 0)) {
                return "";
            }
        }

        StringBuffer url = new StringBuffer();
        url.append("https://").append(ip);
        if (port != null && !"".equals(port)) {
            url.append(":" + port);
        }

        url.append(rest);
        CloseableHttpClient client = getHttpsClient();
        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url.toString());
        httpDelete.setHeader("Content-Type", "application/json");
        httpDelete.setHeader("Cookie", getCookie(ip));
        logger.debug("request line:delete-" + httpDelete.getRequestLine());

        try {
            StringEntity entity = new StringEntity(payload, "UTF-8");
            entity.setContentType("text/json");
            entity.setContentEncoding(new BasicHeader("Content-Type", "application/json"));
            httpDelete.setEntity(entity);
            HttpResponse httpResponse = client.execute(httpDelete);
            String var13 = getResponseString(httpResponse);
            return var13;
        } catch (IOException var21) {
            var21.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException var20) {
                var20.printStackTrace();
            }

        }

        return "";
    }


    public static String hsHttpPost(String ip, String port, String rest, String payload) throws Exception {
        Gson gson = new Gson();
        if (!isLogin(ip, port)) {
            FwLogin login = new FwLogin();
            String loginUrl = gson.toJson(login);
            if (!login(ip, port, loginUrl, 0)) {
                return "";
            }
        }

        StringBuffer url = new StringBuffer();
        url.append("https://").append(ip);
        if (port != null && !"".equals(port)) {
            url.append(":" + port);
        }

        url.append(rest);
        CloseableHttpClient client = getHttpsClient();
        HttpPost httpPost = new HttpPost(url.toString());
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Cookie", getCookie(ip));
        logger.debug("request line:post-" + httpPost.getRequestLine());

        try {
            StringEntity entity = new StringEntity(payload, "UTF-8");
            entity.setContentType("text/json");
            entity.setContentEncoding(new BasicHeader("Content-Type", "application/json"));
            httpPost.setEntity(entity);
            HttpResponse httpResponse = client.execute(httpPost);
            String var11 = getResponseString(httpResponse);
            return var11;
        } catch (IOException var19) {
            logger.error(var19);
        } finally {
            try {
                client.close();
            } catch (IOException var18) {
                logger.error(var18);
            }

        }

        return "";
    }

    public static String hsHttpDelete(String ip, String port, String rest, String payload) throws Exception {
        Gson gson = new Gson();
        if (!isLogin(ip, port)) {
            FwLogin login = new FwLogin();
            String loginUrl = gson.toJson(login);
            if (!login(ip, port, loginUrl, 0)) {
                return "";
            }
        }

        StringBuffer url = new StringBuffer();
        url.append("https://").append(ip);
        if (port != null && !"".equals(port)) {
            url.append(":" + port);
        }

        url.append(rest);
        CloseableHttpClient client = getHttpsClient();
        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url.toString());
        httpDelete.setHeader("Content-Type", "application/json");
        httpDelete.setHeader("Cookie", getCookie(ip));
        logger.debug("request line:delete-" + httpDelete.getRequestLine());

        try {
            StringEntity entity = new StringEntity(payload, "UTF-8");
            entity.setContentType("text/json");
            entity.setContentEncoding(new BasicHeader("Content-Type", "application/json"));
            httpDelete.setEntity(entity);
            HttpResponse httpResponse = client.execute(httpDelete);
            String var11 = getResponseString(httpResponse);
            return var11;
        } catch (IOException var19) {
            var19.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException var18) {
                var18.printStackTrace();
            }

        }

        return "";
    }
}
