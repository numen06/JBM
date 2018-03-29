package service.test.event;

import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;
import com.td.framework.event.bean.IEventListener;
import com.td.util.TimeUtil;

@Service
public class RunListener implements IEventListener {

	@Subscribe
	public void onApplicationEvent(WalkEvent event) {
		WalkEvent weak = (WalkEvent) event;
		System.err.println("现在是上海时间1：" + TimeUtil.format(weak.times));
	}

	@Subscribe
	public void onApplicationEvent2(WalkEvent event) {
		WalkEvent weak = (WalkEvent) event;
		System.err.println("现在是上海时间2：" + TimeUtil.format(weak.times));
	}

}
