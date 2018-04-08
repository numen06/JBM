package td.framework.boot.autoconfigure.tio.handler;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

public interface HttpResponseHandler {
	/**
	 * 处理请求
	 * 
	 * @param packet
	 * @return 可以为null
	 * @throws Exception
	 * @author tanyaowu
	 */
	public HttpRequest handler(HttpResponse packet) throws Exception;
}
