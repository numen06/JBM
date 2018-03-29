package service.test.event;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.td.framework.service.support.RemoteEventSupport;

@Service
public class WalkAware implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Autowired
	private RemoteEventSupport remoteEventSupport;

	public void nextTime(Date address) throws Exception {
		List<WalkEvent> list = Lists.newArrayList();
		for (int i = 0; i < 1000; i++) {
			list.add(new WalkEvent(this, address));
		}
		remoteEventSupport.postEvent(list);
	}
}