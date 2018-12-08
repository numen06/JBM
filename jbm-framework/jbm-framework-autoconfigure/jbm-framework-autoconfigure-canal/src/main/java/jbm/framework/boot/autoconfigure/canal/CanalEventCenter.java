package jbm.framework.boot.autoconfigure.canal;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.alibaba.google.common.util.concurrent.AbstractScheduledService;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.alibaba.otter.canal.protocol.CanalEntry.EntryType;
import com.alibaba.otter.canal.protocol.CanalEntry.EventType;
import com.alibaba.otter.canal.protocol.Message;

import jbm.framework.boot.autoconfigure.canal.event.DeleteCanalEvent;
import jbm.framework.boot.autoconfigure.canal.event.InsertCanalEvent;
import jbm.framework.boot.autoconfigure.canal.event.UpdateCanalEvent;

public class CanalEventCenter extends AbstractScheduledService implements ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(CanalEventCenter.class);
	private ApplicationContext applicationContext;
	@Resource
	private CanalTemplate canalTemplate;

	public CanalEventCenter(CanalTemplate canalTemplate) {
		super();
		this.canalTemplate = canalTemplate;
	}

	@Override
	protected void runOneIteration() throws Exception {
		try {
			int batchSize = 1000;
//            Message message = connector.get(batchSize);
			Message message = canalTemplate.getCanalConnector().getWithoutAck(batchSize);
			long batchId = message.getId();
			logger.debug("scheduled_batchId=" + batchId);
			try {
				List<Entry> entries = message.getEntries();
				if (batchId != -1 && entries.size() > 0) {
					entries.forEach(entry -> {
						if (entry.getEntryType() == EntryType.ROWDATA) {
							publishCanalEvent(entry);
						}
					});
				}
				canalTemplate.getCanalConnector().ack(batchId);
			} catch (Exception e) {
				logger.error("发送监听事件失败！batchId回滚,batchId=" + batchId, e);
				canalTemplate.getCanalConnector().rollback(batchId);
			}
		} catch (Exception e) {
			logger.error("canal_scheduled异常！", e);
		}
	}

	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedDelaySchedule(1000, 100, TimeUnit.MICROSECONDS);
	}

	private void publishCanalEvent(Entry entry) {
		EventType eventType = entry.getHeader().getEventType();
		switch (eventType) {
		case INSERT:
			applicationContext.publishEvent(new InsertCanalEvent(entry));
			break;
		case UPDATE:
			applicationContext.publishEvent(new UpdateCanalEvent(entry));
			break;
		case DELETE:
			applicationContext.publishEvent(new DeleteCanalEvent(entry));
			break;
		default:
			break;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
