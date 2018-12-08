package jbm.framework.boot.autoconfigure.canal;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.google.common.collect.Lists;

/**
 * @author wesley.zhang
 * @date 2018年11月29日 下午4:06:38
 */
@Component
public class CanalTemplate implements DisposableBean, InitializingBean {
	private static final Logger logger = LoggerFactory.getLogger(CanalTemplate.class);
	/**
	 * canal的容器
	 */
	private CanalConnector canalConnector;

	private CanalProperties canalProperties;

	public CanalTemplate(CanalProperties canalProperties) {
		super();
		this.canalProperties = canalProperties;
	}

	public CanalConnector getCanalConnector() {
		return canalConnector;
	}

	@Override
	public void destroy() throws Exception {
		if (canalConnector != null) {
			canalConnector.disconnect();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		canalConnector = CanalConnectors.newClusterConnector(
				Lists.newArrayList(
						new InetSocketAddress(canalProperties.getHost(), Integer.valueOf(canalProperties.getPort()))),
				canalProperties.getDestination(), canalProperties.getUsername(), canalProperties.getPassword());
		canalConnector.connect();
		// 指定filter，格式 {database}.{table}，这里不做过滤，过滤操作留给用户
		canalConnector.subscribe();
		// 回滚寻找上次中断的位置
		canalConnector.rollback();
		logger.info("canal客户端启动成功");
	}
}
