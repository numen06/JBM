package com.jbm.framework.metadata.usage.bean;

import java.util.concurrent.TimeUnit;

public abstract class AbstraceFuture implements IFuture {

    /**
     * A number of seconds to wait between two deadlock controls ( 5 seconds )
     */
    private static final long DEAD_LOCK_CHECK_INTERVAL = 5000L;

    private final Object lock;

    private boolean ready = false;

    private int waiters;

    private Object result;

    public AbstraceFuture() {
        super();
        this.lock = this;
    }

    @Override
    public IFuture await() throws InterruptedException {
        synchronized (lock) {
            while (!ready) {
                waiters++;
                try {
                    lock.wait(DEAD_LOCK_CHECK_INTERVAL);
                } finally {
                    waiters--;
                }
            }
        }
        return this;
    }

    private boolean await0(long timeoutMillis, boolean interruptable) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeoutMillis;

        if (endTime < 0) {
            endTime = Long.MAX_VALUE;
        }

        synchronized (lock) {
            if (ready) {
                return ready;
            } else if (timeoutMillis <= 0) {
                return ready;
            }

            waiters++;

            try {
                for (; ; ) {
                    try {
                        long timeOut = Math.min(timeoutMillis, DEAD_LOCK_CHECK_INTERVAL);
                        lock.wait(timeOut);
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        }
                    }

                    if (ready) {
                        return true;
                    }

                    if (endTime < System.currentTimeMillis()) {
                        return ready;
                    }
                }
            } finally {
                waiters--;
            }
        }
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await(unit.toMillis(timeout));
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(timeoutMillis, true);
    }

    @Override
    public boolean isDone() {
        synchronized (lock) {
            return ready;
        }
    }

    /**
     * Returns the result of the asynchronous operation.
     */
    protected Object getValue() {
        synchronized (lock) {
            return result;
        }
    }

    public void setValue(Object newValue) {
        synchronized (lock) {
            // Allow only once.
            if (ready) {
                return;
            }

            result = newValue;
            ready = true;
            if (waiters > 0) {
                lock.notifyAll();
            }
        }
    }

}
