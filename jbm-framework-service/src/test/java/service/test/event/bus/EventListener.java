package service.test.event.bus;

import com.google.common.eventbus.Subscribe;

public class EventListener {
	public int lastMessage = 0;

	@Subscribe
	public void listen(TestEvent<Integer> event) {
		lastMessage = event.getMessage();
		System.out.println("Message1:" + lastMessage + "=" + event.getVal());
	}

	@Subscribe
	public void listen112(TestEvent<String> event) {
		lastMessage = event.getMessage();
		System.out.println("Message1:" + lastMessage + "=" + event.getVal());
	}

	@Subscribe
	public void test(TestEventTwo event) {
		lastMessage = event.getMessage();
		System.out.println("Message2:" + lastMessage);
	}

	@Subscribe
	public void test(TestEventExt event) {
		lastMessage = event.getMessage();
		System.out.println("Message ext:" + lastMessage);
	}

	public int getLastMessage() {
		return lastMessage;
	}
}
