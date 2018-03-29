package service.test.event.bus;

public class TestEvent<T> {
	private final int message;

	private T val;

	public TestEvent(int message) {
		this.message = message;
		System.out.println("event message:" + message);
	}

	public TestEvent(T val) {
		this.message = 0;
		this.val = val;
	}

	public int getMessage() {
		return message;
	}

	public T getVal() {
		return val;
	}

	public void setVal(T val) {
		this.val = val;
	}

}