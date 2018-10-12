package com.inspur.eipatomapi.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inspur.eipatomapi.entity.Cookie;
import com.inspur.eipatomapi.entity.FwLogin;
import com.inspur.eipatomapi.entity.FwLoginResponseBody;
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
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class HsHttpClient {

    private final static Logger logger = LoggerFactory.getLogger(HsHttpClient.class);

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
			try {
				byte[] payload;

                payload = readStream(instream);

				if (0 == payload.length) {
					return getJson(response.getStatusLine().getStatusCode());
				}
				JSONObject jo = new JSONObject(new String(payload));
				return jo.toString();
			} catch (Exception e) {
                // TODO Auto-generated catch block
                logger.error("Exception.",e);
                return "";
            }
		} else {
			return getJson(response.getStatusLine().getStatusCode());
		}

	}

	private static String getJson(int code) {
		if (code == HsConstants.STATUS_CODE_200 || code == HsConstants.STATUS_CODE_204) {
			return "{\"success\":true, \"result\":[], \"exception\":{}}";
		} else {
			return "{\"success\":false, \"result\":[], \"exception\":{}}";
		}
	}

	private static boolean isLogin(String ip, String port) {
		logger.info("判断登录防火墙状态:" + ip + "" + port);
		if (!isHaveCookie(ip)) {
			logger.info("无防火墙cookie！");
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
		logger.debug("request line:" + httpGet.getRequestLine());
		FwLoginResponseBody body = new FwLoginResponseBody();
		try {
			// jiangfw 软硬件防火墙切换--20170310
			Gson gson = new Gson();
			String strlogin = EntityUtils.toString(client.execute(httpGet).getEntity());
			boolean success;
			if(strlogin.contains("\"success\":true") || strlogin.contains("\"success\" : true")){
				success = true;
			}else{
				success = false;
			}
			body.setSuccess(success);
			// jiangfw 软硬件防火墙切换--20170310
			
			//Gson gson = new Gson();
			//body = gson.fromJson(EntityUtils.toString(client.execute(httpGet).getEntity()), new TypeToken<LoginResponseBody>() {
			//}.getType());
			if(body.isSuccess()){
				logger.info("已登录防火墙！");
			}else{
				logger.info("未登录防火墙!");
			}
			return body.isSuccess();
		} catch (ClientProtocolException e1) {
			logger.error("Failed to login.", e1);
			return false;
		} catch (IOException ex) {
			logger.error("Io exception when login.",ex);
			return false;
		} finally {
			try {
				if (client != null) {
					client.close();
				}
			} catch (IOException e) {
				logger.error("Exception when login.",e);
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

			Cookie cookie = new Cookie(token, platform, hw_platform, host_name, company, oemid, vsysid, vsysname, role, license, httpProtocol, soft_version, username, overseaLicense, HS_frame_lang);
			logger.info(cookie.toString());
			return HsConstants.FROM_ROOT_SYS + cookie.toString();

		} else {
			logger.error("no found result:" + jo);
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
        //指定信任密钥存储对象和连接套接字工厂
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            //信任任何链接
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
        //设置连接管理器
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);
        //构建客户端
        return HttpClientBuilder.create().setConnectionManager(connManager).build();
    }    
	
	private static boolean httpLogin(String url, String ip, String json) throws Exception {
		CloseableHttpClient httpclient = getHttpsClient();
		logger.debug("httpLogin内部：开始登陆,URL:"+url + " IP:"+ip +" json:" + json );
		
//		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(url);
		httpPost.addHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);

		StringEntity se = new StringEntity(json, HTTP.UTF_8);
		se.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
		se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
		httpPost.setEntity(se);
		CloseableHttpResponse response = httpclient.execute(httpPost);
		InputStream instream = null;
		try {
			HttpEntity entity = response.getEntity();
			// do something useful with the response body
			// and ensure it is fully consumed
			logger.debug("HttpEntity:" + entity.toString());

			// If the response does not enclose an entity, there is no need
			// to bother about connection release
			if (entity != null) {
				instream = entity.getContent();
				byte[] payload = readStream(instream);
				JSONObject jo = new JSONObject(new String(payload));

                Gson gson = new Gson();
                Object userpw = gson.fromJson(json, FwLogin.class);
				jo.put("user",((FwLogin) userpw).getPassword());
				String loginResult = loginCookieParser(jo);

				if (loginResult != null && loginResult != "") {
					// loginCookieParser
					logger.debug("httpLogin内部： COOKIE信息  IP:"+ip +" loginResult:" + loginResult );
					putCookie(ip, loginResult);
					logger.debug("httpLogin内部：登陆成功");
					return true;
				} else {
					logger.debug("httpLogin内部：登陆失败");
					return false;
				}
			}
			// do something useful with the response
		} catch (IOException ex) {
			// In case of an IOException the connection will be released
			// back to the connection manager automatically
			logger.error("Exception when login.",ex);
		} finally {
			// Closing the input stream will trigger connection release
			if (null != instream) {
				instream.close();
			}
			if (null != response) {
				response.close();
			}
		}
		logger.debug("httpLogin内部：登陆失败");
		return false;

	}

	@SuppressWarnings("finally")
	private static boolean login(String ip, String port, String login, int tryTimes) throws Exception {
		logger.info("登录防火墙:" + ip + "" + port + "" + login);
		
		StringBuffer url = new StringBuffer();
		url.append(HsConstants.HTTPS).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(HsConstants.REST_LOGIN);
		// String json =
		// "{\"userName\": \"hillstone\", \"password\": \"hillstone\", \"ifVsysId\": \"0\", \"vrId\": \"1\", \"lang\": \"zh_CN\"}";
		boolean flag = true;
		try {
			tryTimes++;
			logger.info("登录防火墙参数: url:"+ url.toString()+ "ip:"+ ip + "login:" + login );			
			flag = httpLogin(url.toString(), ip, login);
			if (flag) {
				logger.info("登录防火墙: 成功!");
				return true;
			}
			logger.info("登录防火墙: 失败!");
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

		logger.debug("request line:get-" + httpGet.getRequestLine());
		try {
			HttpResponse httpResponse = client.execute(httpGet);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			System.out.println(e);
			logger.debug("Io Exception when get.",e);
			return "";
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				logger.debug("IO Exception when get.",e);
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

		logger.debug("request line:post-" + httpPost.getRequestLine());
		try {
			// payload
			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpPost.setEntity(entity);
			HttpResponse httpResponse = client.execute(httpPost);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			logger.error("IO Exception when post.",e);
			return "";
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				logger.error("IO Exception when post.",e);
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

		logger.debug("request line:put-" + httpPut.getRequestLine());
		try {
			// payload
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
				logger.error("IO Exception when put.",e);
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

		CloseableHttpClient client = HttpClients.createDefault();
		HttpPut httpPut = new HttpPut(url.toString());
		RequestConfig config = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build();
		httpPut.setConfig(config);
		httpPut.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpPut.setHeader("Cookie", getCookie(ip));

		logger.debug("request line:put-" + httpPut.getRequestLine());
		try {
			// payload
			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpPut.setEntity(entity);

			HttpResponse httpResponse = client.execute(httpPut);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			logger.error("IO Exception when put.",e);
			return e.toString();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				logger.error("IO Exception when put.",e);
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

		logger.debug("request line:delete-" + httpDelete.getRequestLine());
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
		
		
		/*Gson gson = new Gson();
		if (!isLogin(ip, port)) {
			Login login = new Login();
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

		logger.debug("request line:delete-" + httpDelete.getRequestLine());
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

		return "";*/
	}

	public static String hsHttpGet(String ip, String port, String rest) throws Exception {
		Gson gson = new Gson();

		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
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

		logger.debug("request line:get-" + httpGet.getRequestLine());
		try {
			HttpResponse httpResponse = client.execute(httpGet);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			System.out.println(e);
			logger.debug("IO Exception when get.",e);
			return "";
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				logger.debug("IO Exception when get.",e);
			}
		}

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
		url.append(HsConstants.HTTPS).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(rest);

		CloseableHttpClient client = getHttpsClient();
		HttpPost httpPost = new HttpPost(url.toString());

		httpPost.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpPost.setHeader("Cookie", getCookie(ip));

		logger.debug("request line:post-" + httpPost.getRequestLine());
		try {
			// payload
			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpPost.setEntity(entity);

			HttpResponse httpResponse = client.execute(httpPost);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			logger.error("IO Exception when post.",e);
			return "";
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				logger.error("IO Exception when post.",e);
			}
		}
	}

	public static String hsHttpPut(String ip, String port, String rest, String payload) throws Exception {
		Gson gson = new Gson();
		if (!isLogin(ip, port)) {
			FwLogin login = new FwLogin();
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

		logger.debug("request line:put-" + httpPut.getRequestLine());
		try {
			// payload
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
				logger.error("IO Exception when put.",e);
			}
		}
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

		CloseableHttpClient client = HttpClients.createDefault();
		HttpPut httpPut = new HttpPut(url.toString());
		RequestConfig config = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout).build();
		httpPut.setConfig(config);
		httpPut.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpPut.setHeader("Cookie", getCookie(ip));

		logger.debug("request line:put-" + httpPut.getRequestLine());
		try {
			// payload
			StringEntity entity = new StringEntity(payload, HTTP.UTF_8);
			entity.setContentType(HsConstants.CONTENT_TYPE_TEXT_JSON);
			entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON));
			httpPut.setEntity(entity);

			HttpResponse httpResponse = client.execute(httpPut);
			return getResponseString(httpResponse);

		} catch (IOException e) {
			logger.error("IO Exception when put.",e);
			return e.toString();
		} finally {
			try {
				client.close();
			} catch (IOException e) {
				logger.error("IO Exception when put.",e);
			}
		}
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
		url.append(HsConstants.HTTPS).append(ip);
		if (null != port && !"".equals(port)) {
			url.append(HsConstants.COLON+port);
		}
		url.append(rest);

		CloseableHttpClient client = getHttpsClient();
		HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url.toString());

		httpDelete.setHeader(HTTP.CONTENT_TYPE, HsConstants.APPLICATION_JSON);
		httpDelete.setHeader("Cookie", getCookie(ip));

		logger.debug("request line:delete-" + httpDelete.getRequestLine());
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
