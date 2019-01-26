package com.inspur.eipatomapi.util;

import com.google.gson.Gson;
import com.inspur.eipatomapi.entity.fw.Cookie;
import com.inspur.eipatomapi.entity.fw.FwLogin;
import com.inspur.eipatomapi.entity.fw.FwLoginResponseBody;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
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
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HsHttpClient {

	private static Map<String, String> cookieMap = new HashMap<>();

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
		int len;
		while ((len = inStream.read(buffer)) != -1) {
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
            String ret = ConvertStreamToString(instream);
            log.debug(ret);
            return ret;

		} else {
			return getJson(response.getStatusLine().getStatusCode());
		}

	}


    // Convert stream to string
    public static String ConvertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            log.error("Error=" + e.toString());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                log.error("Error=" + e.toString());
            }
        }
        return sb.toString();

    }

	private static String getJson(int code) {
		if (code == HsConstants.STATUS_CODE_200 || code == HsConstants.STATUS_CODE_204) {
			return "{\"success\":true, \"result\":[], \"exception\":{}}";
		} else {
			return "{\"success\":false, \"result\":[], \"exception\":{}}";
		}
	}

	private static boolean isLogin(String ip, String port) {
		log.debug("Login state:" + ip + "" + port);
		if (!isHaveCookie(ip)) {
			log.info("No cookie！");
			return false;
		}
		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTPS).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(HsConstants.REST_LOGIN);

		CloseableHttpClient client = getHttpsClient();
		HttpGet httpGet = new HttpGet(url.toString());

		httpGet.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpGet.setHeader("Cookie", getCookie(ip));
		log.debug("request line:" + httpGet.getRequestLine());
		FwLoginResponseBody body = new FwLoginResponseBody();
		try {
			Gson gson = new Gson();
			String strlogin = EntityUtils.toString(client.execute(httpGet).getEntity());
			boolean success;
			if(strlogin.contains("\"success\":true") || strlogin.contains("\"success\" : true")){
				success = true;
				log.info("Login success！");
			}else{
				success = false;
				log.info("Not login!");
			}
			body.setSuccess(success);
			return body.isSuccess();
		} catch (ClientProtocolException e1) {
			log.error("Failed to login.", e1);
			return false;
		} catch (IOException ex) {
			log.error("Io exception when login.",ex);
			return false;
		} finally {
			try {
				if (client != null) {
					client.close();
				}
			} catch (IOException e) {
				log.error("Exception when login.",e);
			}
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
//			String username = HsConstants.USER;
            String username = jo.getString("user");
			String overseaLicense = resultJsn.getString("overseaLicense");
			String HS_frame_lang = HsConstants.LANG;

			Cookie cookie = new Cookie(token, platform, hw_platform, host_name, company, oemid, vsysid, vsysname, role,
                    license, httpProtocol, soft_version, username, overseaLicense, HS_frame_lang);
			log.debug(cookie.toString());
			return HsConstants.FROM_ROOT_SYS + cookie.toString();

		} else {
			log.error("no found result:" + jo);
		}

		return "";

	}

	/**
	 * 获取https连接（不验证证书）
	 * @return ret
	 */
	private static CloseableHttpClient getHttpsClient() {
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();
        ConnectionSocketFactory plainSF = new PlainConnectionSocketFactory();
        registryBuilder.register("http", plainSF);
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            TrustStrategy anyTrustStrategy = new TrustStrategy() {

				@Override
				public boolean isTrusted(
						java.security.cert.X509Certificate[] arg0, String arg1)
						throws java.security.cert.CertificateException {
					// TODO Auto-generated method stub
					return true;
				}
            };
            SSLContext sslContext = SSLContexts.custom().useTLS().loadTrustMaterial(trustStore, anyTrustStrategy).build();
            LayeredConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            registryBuilder.register("https", sslSF);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        Registry<ConnectionSocketFactory> registry = registryBuilder.build();

        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);

        return HttpClientBuilder.create().setConnectionManager(connManager).build();
    }    
	
	private static boolean httpLogin(String url, String ip, String json) throws Exception {
		CloseableHttpClient httpclient = getHttpsClient();
		log.debug("httpLogin：start login,URL:{} ip:{}",url ,ip );
		
		HttpPost httpPost = new HttpPost(url);
		httpPost.addHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);

		StringEntity se = new StringEntity(json, HTTP.UTF_8);
		se.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
		se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
		httpPost.setEntity(se);
		CloseableHttpResponse response =null ;
		InputStream instream = null;
		try {
			response = httpclient.execute(httpPost);

			HttpEntity entity = response.getEntity();
			// do something useful with the response body
			// and ensure it is fully consumed
			log.debug("HttpEntity:{}", entity.toString());

			// If the response does not enclose an entity, there is no need
			// to bother about connection release
			if (entity != null) {
				instream = entity.getContent();
				byte[] payload = readStream(instream);
				JSONObject jo = new JSONObject(new String(payload));

                Gson gson = new Gson();
                Object userpw = gson.fromJson(json, FwLogin.class);
				jo.put("user",((FwLogin) userpw).getUserName());
				String loginResult = loginCookieParser(jo);

				if (loginResult != null && loginResult != "") {
					// loginCookieParser
					log.debug("httpLogin： COOKIE  IP:{} logingResult:{}",ip, loginResult );
					putCookie(ip, loginResult);
					log.debug("httpLogin：login success!");
					return true;
				} else {
					log.debug("httpLogin：Failed to login");
					return false;
				}
			}
			// do something useful with the response
		} catch (IOException ex) {
			// In case of an IOException the connection will be released
			// back to the connection manager automatically
			log.error("Exception when login.",ex);
		} finally {
			// Closing the input stream will trigger connection release
			if (null != instream) {
				instream.close();
			}
			if (null != response) {
				response.close();
			}
		}
		log.debug("httpLogin：failed to login");
		return false;

	}

	@SuppressWarnings("finally")
	private static boolean login(String ip, String port, String login, int tryTimes) throws Exception {
		log.debug("Login firewall:{}:{}" , ip ,port);
		
		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTPS).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(HsConstants.REST_LOGIN);

		boolean flag = true;
		try {
			tryTimes++;
			log.debug("Already login: url:{}, ip:{}", url.toString(), ip );
			flag = httpLogin(url.toString(), ip, login);
			if (flag) {
				log.info("login success!");
				return true;
			}
			log.info("Failde to login!");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        if (!flag) {
            //
            Thread.sleep(1000);
            if (tryTimes < 3) {
                return login(ip, port, login, tryTimes);
            } else {
                return false;
            }
        } else {
            return true;
        }

	}

	public static String hsHttpGet(String ip, String port, String user, String pwd, String rest) throws Exception {
		Gson gson = new Gson();

		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
			if (null != user && !"".equals(user)) {
				login.setUserName(user);
				login.setPassword(pwd);
			}
			String loginUrl = gson.toJson(login);
			if (!login(ip, port, loginUrl, 0)) {
				return "";
			}
		}
		
		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTPS).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(rest);

		CloseableHttpClient client = getHttpsClient();
		HttpGet httpGet = new HttpGet(url.toString());

		httpGet.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpGet.setHeader(HsConstants.HILLTONE_LANGUAGE, HsConstants.LANG);

		httpGet.setHeader("Cookie", getCookie(ip));

		log.debug("request line:get-{}" , httpGet.getRequestLine());
		try {
			HttpResponse httpResponse = client.execute(httpGet);
			//log.info(httpResponse.toString());
			return getResponseString(httpResponse);

		} catch (IOException e) {
			System.out.println(e);
			log.debug("Io Exception when get.",e);
			return "";
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				log.debug("IO Exception when get.",e);
			}
		}
	}

	/**
	 * HTTP
	 * @throws Exception
	 */
	public static String HttpGet(String ip, String port, String user, String pwd, String rest) throws Exception {
		Gson gson = new Gson();

		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
			if (null != user && !"".equals(user)) {
				login.setUserName(user);
				login.setPassword(pwd);
			}
			String loginUrl = gson.toJson(login);
			if (!login(ip, port, loginUrl, 0)) {
				return "";
			}
		}

		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTP).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(rest);

		CloseableHttpClient client = getHttpsClient();
		HttpGet httpGet = new HttpGet(url.toString());

		httpGet.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpGet.setHeader(HsConstants.HILLTONE_LANGUAGE, HsConstants.LANG);

		httpGet.setHeader("Cookie", getCookie(ip));

		log.debug("request line:get-{}" , httpGet.getRequestLine());
		try {
			HttpResponse httpResponse = client.execute(httpGet);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			System.out.println(e);
			log.debug("Io Exception when get.",e);
			return "";
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				log.debug("IO Exception when get.",e);
			}
		}
	}

	public static String hsHttpPost(String ip, String port, String user, String pwd, String rest, String payload) throws Exception {
		Gson gson = new Gson();

		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
			if (null != user && !"".equals(user)) {
				login.setUserName(user);
				login.setPassword(pwd);
			}
			String loginUrl = gson.toJson(login);
			if (!login(ip, port, loginUrl, 0)) {
				return "";
			}
		}
		
		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTPS).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(rest);
		
		CloseableHttpClient client = getHttpsClient();
		HttpPost httpPost = new HttpPost(url.toString());
		
		httpPost.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpPost.setHeader("Cookie", getCookie(ip));

		log.debug("request line:post-{}" , httpPost.getRequestLine());
		try {
			// payload
			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpPost.setEntity(entity);
			HttpResponse httpResponse = client.execute(httpPost);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			log.error("IO Exception when post.",e);
			return "";
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				log.error("IO Exception when post.",e);
			}
		}
	}
	public static String HttpPost(String ip, String port, String user, String pwd, String rest, String payload) throws Exception {
		Gson gson = new Gson();

		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
			if (null != user && !"".equals(user)) {
				login.setUserName(user);
				login.setPassword(pwd);
			}
			String loginUrl = gson.toJson(login);
			if (!login(ip, port, loginUrl, 0)) {
				return "";
			}
		}

		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTP).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(rest);

		CloseableHttpClient client = getHttpsClient();
		HttpPost httpPost = new HttpPost(url.toString());

		httpPost.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpPost.setHeader("Cookie", getCookie(ip));

		log.debug("request line:post-{}" , httpPost.getRequestLine());
		try {
			// payload
			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpPost.setEntity(entity);
			HttpResponse httpResponse = client.execute(httpPost);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			log.error("IO Exception when post.",e);
			return "";
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				log.error("IO Exception when post.",e);
			}
		}
	}

	public static String hsHttpPut(String ip, String port, String user, String pwd, String rest, String payload) throws Exception {
		Gson gson = new Gson();

		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
			if (null != user && !"".equals(user)) {
				login.setUserName(user);
				login.setPassword(pwd);
			}
			String loginUrl = gson.toJson(login);
			if (!login(ip, port, loginUrl, 0)) {
				return "";
			}
		}
		
		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTPS).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(rest);
		
		CloseableHttpClient client = getHttpsClient();
		HttpPut httpPut = new HttpPut(url.toString());

		httpPut.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpPut.setHeader("Cookie", getCookie(ip));

		log.debug("request line:put-{}" , httpPut.getRequestLine());
		try {

			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpPut.setEntity(entity);

			HttpResponse httpResponse = client.execute(httpPut);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			return "";
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				log.error("IO Exception when put.",e);
			}
		}
	}

	public static String hsHttpPut(String ip, String port, String user, String pwd, int timeout, String rest, String payload) throws Exception {
		Gson gson = new Gson();
		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
			if (null != user && !"".equals(user)) {
				login.setUserName(user);
				login.setPassword(pwd);
			}
			String loginUrl = gson.toJson(login);
			if (!login(ip, port, loginUrl, 0)) {
				return "";
			}
		}
		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTP).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(port);
		}
		url.append(rest);

		CloseableHttpClient client = getHttpsClient();;
		HttpPut httpPut = new HttpPut(url.toString());
		RequestConfig config = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build();
		httpPut.setConfig(config);
		httpPut.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpPut.setHeader("Cookie", getCookie(ip));

		log.debug("request line:put-{}" , httpPut.getRequestLine());
		try {
			// payload
			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpPut.setEntity(entity);

			HttpResponse httpResponse = client.execute(httpPut);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			log.error("IO Exception when put.",e);
			return e.toString();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				log.error("IO Exception when put.",e);
			}
		}
	}

	public static String hsHttpDelete(String ip, String port, String user, String pwd, String rest, String payload) throws Exception {

		Gson gson = new Gson();
		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
			if (null != user && !"".equals(user)) {
				login.setUserName(user);
				login.setPassword(pwd);
			}
			String loginUrl = gson.toJson(login);
			if (!login(ip, port, loginUrl, 0)) {
				return "";
			}
		}
		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTPS).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(rest);

		CloseableHttpClient client = getHttpsClient();
		HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url.toString());

		httpDelete.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpDelete.setHeader("Cookie", getCookie(ip));

		log.debug("request line:delete-{}" , httpDelete.getRequestLine());
		try {
			// payload
			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpDelete.setEntity(entity);
			HttpResponse httpResponse = client.execute(httpDelete);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return "";

	}


	public static String hsHttpPut(String ip, String port, int timeout, String rest, String payload) throws Exception {
		Gson gson = new Gson();
		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
			String loginUrl = gson.toJson(login);
			if (!login(ip, port, loginUrl, 0)) {
				return "";
			}
		}
		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTP).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(port);
		}
		url.append(rest);

		CloseableHttpClient client = getHttpsClient();;
		HttpPut httpPut = new HttpPut(url.toString());
		RequestConfig config = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build();
		httpPut.setConfig(config);
		httpPut.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpPut.setHeader("Cookie", getCookie(ip));

		log.debug("request line:put-{}" , httpPut.getRequestLine());
		try {
			// payload
			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpPut.setEntity(entity);

			HttpResponse httpResponse = client.execute(httpPut);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			log.error("IO Exception when put.",e);
			return e.toString();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				log.error("IO Exception when put.",e);
			}
		}
	}

}


@NotThreadSafe
class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
	public static final String METHOD_NAME = "DELETE";

	public String getMethod() {
		return METHOD_NAME;
	}

	public HttpDeleteWithBody(final String uri) {
		super();
		setURI(URI.create(uri));
	}

	public HttpDeleteWithBody(final URI uri) {
		super();
		setURI(uri);
	}

	public HttpDeleteWithBody() {
		super();
	}
}
