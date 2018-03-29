package service.test.event.bus;

public class TestEventTwo {
	private final int message;

	public TestEventTwo(int message) {
		this.message = message;
		System.out.println("event message:" + message);
	}

	public int getMessage() {
		return message;
	}
}