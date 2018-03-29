package com.td.util.iterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.util.concurrent.AbstractExecutionThreadService;

/**
 * 
 * 自动循环转发器
 * 
 * @author wesley
 *
 * @param <T>
 */
public abstract class AutoStateIterator<T> extends AbstractExecutionThreadService implements Iterator<T>, Iterable<T> {

	protected enum State {
		DONE, READY, NOT_READY, FAILED;
	}

	protected State state = State.NOT_READY;

	protected T nextItem = null;

	public T next() {
		if (!hasNext())
			throw new NoSuchElementException();
		state = State.NOT_READY;
		return nextItem;
	}

	public boolean hasNext() {
		switch (state) {
		case FAILED:
			throw new IllegalStateException("Iterator is in failed state");
		case DONE:
			return false;
		case READY:
			return true;
		case NOT_READY:
			break;
		}
		return maybeComputeNext();
	}

	protected abstract T makeNext();

	private boolean maybeComputeNext() {
		state = State.FAILED;
		nextItem = makeNext();
		if (state == State.DONE)
			return false;
		state = State.READY;
		return true;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	protected void resetState() {
		state = State.NOT_READY;
	}

	public State getState() {
		return state;
	}

	protected T allDone() {
		state = State.DONE;
		return null;
	}

	@Override
	public Iterator<T> iterator() {
		return this;
	}

	@Override
	protected void run() throws Exception {
		while (hasNext()) {
			next();
		}
	}
}
