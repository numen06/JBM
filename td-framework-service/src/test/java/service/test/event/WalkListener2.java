package service.test.event;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import com.td.util.TimeUtil;

@Service
public class WalkListener2 implements ApplicationListener<WalkEvent> {

	@Override
	public void onApplicationEvent(WalkEvent event) {
		WalkEvent weak = (WalkEvent) event;
		System.out.println("现在是连云港时间：" + TimeUtil.format(weak.times));
	}
}