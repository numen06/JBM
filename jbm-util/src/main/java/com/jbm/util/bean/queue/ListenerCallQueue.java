package com.jbm.util.bean.queue;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ListenerCallQueue<L> implements Runnable {
    private static final Logger logger = Logger.getLogger(ListenerCallQueue.class.getName());
    private final L listener;
    private final Executor executor;
    private final Queue<Callback<L>> waitQueue = Queues.newArrayDeque();
    private boolean isThreadScheduled;
    ListenerCallQueue(L listener, Executor executor) {
        this.listener = checkNotNull(listener);
        this.executor = checkNotNull(executor);
    }

    /**
     * Enqueues a task to be run.
     */
    synchronized void add(Callback<L> callback) {
        waitQueue.add(callback);
    }

    /**
     * Executes all listeners {@linkplain #add added} prior to this call,
     * serially and in order.
     */
    void execute() {
        boolean scheduleTaskRunner = false;
        synchronized (this) {
            if (!isThreadScheduled) {
                isThreadScheduled = true;
                scheduleTaskRunner = true;
            }
        }
        if (scheduleTaskRunner) {
            try {
                executor.execute(this);
            } catch (RuntimeException e) {
                // reset state in case of an error so that later calls to
                // execute will actually do something
                synchronized (this) {
                    isThreadScheduled = false;
                }
                // Log it and keep going.
                logger.log(Level.SEVERE, "Exception while running callbacks for " + listener + " on " + executor, e);
                throw e;
            }
        }
    }

    @Override
    public void run() {
        boolean stillRunning = true;
        try {
            while (true) {
                Callback<L> nextToRun;
                synchronized (ListenerCallQueue.this) {
                    Preconditions.checkState(isThreadScheduled);
                    nextToRun = waitQueue.poll();
                    if (nextToRun == null) {
                        isThreadScheduled = false;
                        stillRunning = false;
                        break;
                    }
                }
                // Always run while _not_ holding the lock, to avoid deadlocks.
                try {
                    nextToRun.call(listener);
                } catch (RuntimeException e) {
                    // Log it and keep going.
                    logger.log(Level.SEVERE, "Exception while executing callback: " + listener + "." + nextToRun.methodCall, e);
                }
            }
        } finally {
            if (stillRunning) {
                // An Error is bubbling up, we should mark ourselves as no
                // longer
                // running, that way if anyone tries to keep using us we won't
                // be
                // corrupted.
                synchronized (ListenerCallQueue.this) {
                    isThreadScheduled = false;
                }
            }
        }
    }

    abstract static class Callback<L> {
        private final String methodCall;

        Callback(String methodCall) {
            this.methodCall = methodCall;
        }

        abstract void call(L listener);

        /**
         * Helper method to add this callback to all the queues.
         */
        void enqueueOn(Iterable<ListenerCallQueue<L>> queues) {
            for (ListenerCallQueue<L> queue : queues) {
                queue.add(this);
            }
        }
    }
}
