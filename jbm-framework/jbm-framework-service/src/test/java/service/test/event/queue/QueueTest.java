package service.test.event.queue;

import java.util.Queue;

import com.google.common.collect.Queues;

public class QueueTest implements Comparable<QueueTest> {
	private String name;
	private int population;

	public QueueTest(String name, int population) {
		super();
		this.name = name;
		this.population = population;
	}

	public String getName() {
		return this.name;
	}

	public int getPopulation() {
		return this.population;
	}

	public String toString() {
		return getName() + " - " + getPopulation();
	}

	public static void main(String args[]) {

		Queue<QueueTest> priorityQueue = Queues.newPriorityBlockingQueue();

		QueueTest t1 = new QueueTest("t1", 1);
		QueueTest t3 = new QueueTest("t3", 3);
		QueueTest t2 = new QueueTest("t2", 2);
		QueueTest t4 = new QueueTest("t4", 0);
		priorityQueue.add(t1);
		priorityQueue.add(t3);
		priorityQueue.add(t2);
		priorityQueue.add(t4);
		System.out.println(priorityQueue.poll().toString());
	}

	@Override
	public int compareTo(QueueTest o2) {
		QueueTest o1 = this;
		int numbera = o1.getPopulation();
		int numberb = o2.getPopulation();
		if (numberb > numbera) {
			return -1;
		} else if (numberb < numbera) {
			return 1;
		} else {
			return 0;
		}
	}
}
