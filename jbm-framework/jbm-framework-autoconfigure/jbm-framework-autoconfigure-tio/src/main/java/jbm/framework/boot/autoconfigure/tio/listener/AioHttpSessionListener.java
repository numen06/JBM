package jbm.framework.boot.autoconfigure.tio.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.http.common.HttpConfig;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.session.HttpSession;

public class AioHttpSessionListener implements org.tio.http.server.session.HttpSessionListener {

	private static Logger log = LoggerFactory.getLogger(AioHttpSessionListener.class);

	@Override
	public void doAfterCreated(HttpRequest request, HttpSession session, HttpConfig httpConfig) {
		log.info("有一个客户端正在连接{},SessionId:{}", request.getHeaderString(), session.getId());
	}

}
