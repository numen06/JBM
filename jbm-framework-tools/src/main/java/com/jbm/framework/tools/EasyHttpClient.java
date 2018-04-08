package com.jbm.framework.tools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.jbm.framework.metadata.exceptions.http.HttpConnectionException;
import com.jbm.framework.metadata.exceptions.http.HttpRequestException;
import com.jbm.framework.metadata.usage.bean.http.ResponseWrapper;

/**
 * 简单的HTTP调用接口方法
 * 
 * @author wesley
 * 
 */
public class EasyHttpClient implements IHttpClient {
	private static final Logger LOG = LoggerFactory.getLogger(EasyHttpClient.class);
	private static final String KEYWORDS_CONNECT_TIMED_OUT = "connect timed out";
	private static final String KEYWORDS_READ_TIMED_OUT = "Read timed out";

	private int _maxRetryTimes = 0;
	private String _authCode;
	private HttpProxy _proxy;
	private static final OkHttpClient _client = new OkHttpClient();

	static {
		// 设置连接时间
		_client.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
		// 设置读取时间
		_client.setReadTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.MILLISECONDS);

		TrustManager[] tmCerts = new javax.net.ssl.TrustManager[1];
		tmCerts[0] = new SimpleTrustManager();
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, tmCerts, null);
			_client.setSslSocketFactory(sslContext.getSocketFactory());

			HostnameVerifier hostnameVerifier = new SimpleHostnameVerifier();
			_client.setHostnameVerifier(hostnameVerifier);
		} catch (Exception e) {
		}
	}

	/**
	 * 默认的重连次数是 3
	 */
	public EasyHttpClient(String authCode) {
		this(authCode, DEFAULT_MAX_RETRY_TIMES, null);
	}

	public EasyHttpClient(String authCode, int maxRetryTimes, HttpProxy proxy) {
		this._maxRetryTimes = maxRetryTimes;
		LOG.info("Created instance with _maxRetryTimes = " + _maxRetryTimes);

		this._authCode = authCode;
		this._proxy = proxy;

		if (null != _proxy && _proxy.isAuthenticationNeeded()) {
			Authenticator.setDefault(new SimpleProxyAuthenticator(_proxy.getUsername(), _proxy.getPassword()));
		}

		initSSL();
	}

	public ResponseWrapper sendGet(String url) throws HttpConnectionException, HttpRequestException {
		return doRequest(url, null, RequestMethod.GET);
	}

	public ResponseWrapper sendDelete(String url) throws HttpConnectionException, HttpRequestException {
		return doRequest(url, null, RequestMethod.DELETE);
	}

	public ResponseWrapper sendPost(String url, String content) throws HttpConnectionException, HttpRequestException {
		return doRequest(url, content, RequestMethod.POST);
	}

	public ResponseWrapper sendPut(String url, String content) throws HttpConnectionException, HttpRequestException {
		return doRequest(url, content, RequestMethod.PUT);
	}

	public ResponseWrapper sendFormPost(String url, String content) throws HttpConnectionException, HttpRequestException {
		return doRequest(url, content, RequestMethod.POST, CONTENT_TYPE_FORM);
	}

	public ResponseWrapper sendFormPut(String url, String content) throws HttpConnectionException, HttpRequestException {
		return doRequest(url, content, RequestMethod.PUT, CONTENT_TYPE_FORM);
	}

	public ResponseWrapper doRequest(String url, String content, RequestMethod method) throws HttpConnectionException, HttpRequestException {
		return doRequest(url, content, method, CONTENT_TYPE_JSON);
	}

	public ResponseWrapper doRequest(String url, String content, RequestMethod method, String contentType) throws HttpConnectionException, HttpRequestException {
		ResponseWrapper response = null;
		for (int retryTimes = 0;; retryTimes++) {
			try {
				response = _doRequest(url, content, method, contentType);
				break;
			} catch (SocketTimeoutException e) {
				if (KEYWORDS_READ_TIMED_OUT.equals(e.getMessage())) {
					// Read timed out. For push, maybe should not re-send.
					throw new HttpConnectionException(READ_TIMED_OUT_MESSAGE, e, true);
				} else { // connect timed out
					if (retryTimes >= _maxRetryTimes) {
						throw new HttpConnectionException(CONNECT_TIMED_OUT_MESSAGE, e, retryTimes);
					} else {
						LOG.debug("connect timed out - retry again - " + (retryTimes + 1));
					}
				}
			}
		}
		return response;
	}

	public static void main(String[] args) throws UnsupportedEncodingException {
		String en = URLEncodedUtils.format(Lists.newArrayList(new BasicNameValuePair("theCityCode", "长沙")), CHARSET).toString();
		System.out.println(en);
	}

	@SuppressWarnings("unchecked")
	private ResponseWrapper _doRequest(String url, String content, RequestMethod method, String contentType) throws HttpConnectionException, HttpRequestException,
		SocketTimeoutException {

		LOG.debug("Send request - " + method.toString() + " " + url);
		if (null != content) {
			LOG.debug("Request Content - " + content);
		}

		// HttpURLConnection conn = null;
		// OutputStream out = null;
		// StringBuffer sb = new StringBuffer();
		Builder requestBuilder = new Request.Builder().url(url);
		Request request = null;
		ResponseWrapper wrapper = new ResponseWrapper();

		try {
			if (null != _proxy) {
				_client.setProxy(_proxy.getNetProxy());
				// conn = (HttpURLConnection)
				// aUrl.openConnection(_proxy.getNetProxy());
				if (_proxy.isAuthenticationNeeded()) {
					requestBuilder.addHeader("Proxy-Authorization", _proxy.getProxyAuthorization());
				}
			} else {
				// conn = (HttpURLConnection) aUrl.openConnection();
			}
			// conn.setRequestMethod(method.name());
			requestBuilder.addHeader("User-Agent", DEF_USER_AGENT);
			requestBuilder.addHeader("Connection", "Keep-Alive");
			requestBuilder.addHeader("Accept-Charset", CHARSET);
			requestBuilder.addHeader("Charset", CHARSET);
			requestBuilder.addHeader("Authorization", _authCode);
			requestBuilder.addHeader("Content-Type", contentType);

			if (RequestMethod.GET == method) {
				requestBuilder.get();
			} else if (RequestMethod.DELETE == method) {
				requestBuilder.delete();
			} else if (RequestMethod.POST == method || RequestMethod.PUT == method) {
				// 判断是不是form提交
				if (contentType.equals(CONTENT_TYPE_FORM)) {
					Gson gson = new Gson();
					FormEncodingBuilder formEncodingBuilder = new FormEncodingBuilder();
					Map<String, String> gist = gson.fromJson(content, Map.class);
					for (String key : gist.keySet()) {
						formEncodingBuilder.add(key, gist.get(key));
					}
					RequestBody formBody = formEncodingBuilder.build();
					requestBuilder.post(formBody);
				} else {
					requestBuilder.post(RequestBody.create(MediaType.parse(contentType), content));
				}
			}
			// 完成请求组装之后编译
			request = requestBuilder.build();
			Response response = _client.newCall(request).execute();
			int status = response.code();

			String responseContent = response.body().string();
			wrapper.responseCode = status;
			wrapper.responseContent = responseContent;

			String quota = response.header(RATE_LIMIT_QUOTA);
			String remaining = response.header(RATE_LIMIT_Remaining);
			String reset = response.header(RATE_LIMIT_Reset);
			wrapper.setRateLimit(quota, remaining, reset);

			if (status >= 200 && status < 300) {
				LOG.debug("Succeed to get response OK - responseCode:" + status);
				LOG.debug("Response Content - " + responseContent);

			} else if (status >= 300 && status < 400) {
				LOG.warn("Normal response but unexpected - responseCode:" + status + ", responseContent:" + responseContent);

			} else {
				LOG.warn("Got error response - responseCode:" + status + ", responseContent:" + responseContent);

				switch (status) {
				case 400:
					LOG.error("Your request params is invalid. Please check them according to error message.");
					wrapper.setErrorObject();
					break;
				case 401:
					LOG.error("Authentication failed! Please check authentication params according to docs.");
					wrapper.setErrorObject();
					break;
				case 403:
					LOG.error("Request is forbidden! Maybe your appkey is listed in blacklist or your params is invalid.");
					wrapper.setErrorObject();
					break;
				case 410:
					LOG.error("Request resource is no longer in service. Please according to notice on official website.");
					wrapper.setErrorObject();
				case 429:
					LOG.error("Too many requests! Please review your appkey's request quota.");
					wrapper.setErrorObject();
					break;
				case 500:
				case 502:
				case 503:
				case 504:
					LOG.error("Seems encountered server error. Maybe Http Service is in maintenance? Please retry later.");
					break;
				default:
					LOG.error("Unexpected response.");
				}

				throw new HttpRequestException(wrapper);
			}

		} catch (SocketTimeoutException e) {
			if (e.getMessage().contains(KEYWORDS_CONNECT_TIMED_OUT)) {
				throw e;
			} else if (e.getMessage().contains(KEYWORDS_READ_TIMED_OUT)) {
				throw new SocketTimeoutException(KEYWORDS_READ_TIMED_OUT);
			}
			LOG.debug(IO_ERROR_MESSAGE, e);
			throw new HttpConnectionException(IO_ERROR_MESSAGE, e);

		} catch (IOException e) {
			LOG.debug(IO_ERROR_MESSAGE, e);
			throw new HttpConnectionException(IO_ERROR_MESSAGE, e);
		}

		return wrapper;
	}

	protected void initSSL() {
		TrustManager[] tmCerts = new javax.net.ssl.TrustManager[1];
		tmCerts[0] = new SimpleTrustManager();
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, tmCerts, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

			HostnameVerifier hostnameVerifier = new SimpleHostnameVerifier();
			HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
		} catch (Exception e) {
			LOG.error("Init SSL error", e);
		}
	}

	private static class SimpleHostnameVerifier implements HostnameVerifier {

		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}

	}

	private static class SimpleTrustManager implements TrustManager, X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			return;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			return;
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}
	}

	private static class SimpleProxyAuthenticator extends java.net.Authenticator {
		private String username;
		private String password;

		public SimpleProxyAuthenticator(String username, String password) {
			this.username = username;
			this.password = password;
		}

		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(this.username, this.password.toCharArray());
		}
	}
}
