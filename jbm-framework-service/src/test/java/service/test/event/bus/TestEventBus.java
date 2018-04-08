package service.test.event.bus;

import org.junit.Test;

import com.google.common.eventbus.EventBus;

public class TestEventBus {
	@Test
	public void testReceiveEvent() throws Exception {

		EventBus eventBus = new EventBus("/test");
		EventListener listener = new EventListener();

		eventBus.register(listener);

		eventBus.post(new TestEvent<String>(200));
		eventBus.post(new TestEventTwo(300));
		eventBus.post(new TestEvent<Integer>(400));
		TestEvent te = new TestEventExt(400);
		eventBus.post(te);

		System.out.println("LastMessage:" + listener.getLastMessage());
	}
}
