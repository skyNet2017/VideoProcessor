package com.hss01248.videocompress;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Serializes compress jobs: one running at a time, others wait in FIFO order.
 */
class CompressTaskQueue {

    private final Queue<CompressTask> pending = new LinkedList<>();
    private boolean running;

    void enqueue(CompressTask task, Runnable runner) {
        synchronized (this) {
            pending.offer(task);
        }
        drain(runner);
    }

    synchronized int getPendingCount() {
        return pending.size() + (running ? 1 : 0);
    }

    synchronized boolean isRunning() {
        return running;
    }

    /**
     * Starts the next task if idle. The runner executes outside the lock so that
     * synchronous compression (async=false) does not block other queue callers.
     */
    private void drain(Runnable runner) {
        synchronized (this) {
            if (running || pending.isEmpty()) {
                return;
            }
            running = true;
        }
        runner.run();
    }

    synchronized CompressTask pollNext() {
        return pending.poll();
    }

    void onTaskFinished(Runnable runner) {
        synchronized (this) {
            running = false;
        }
        drain(runner);
    }

    /**
     * Removes waiting tasks whose input path matches (not the job currently executing).
     */
    synchronized List<CompressTask> removePendingByInputPath(String normalizedInputPath) {
        List<CompressTask> removed = new ArrayList<>();
        Iterator<CompressTask> iterator = pending.iterator();
        while (iterator.hasNext()) {
            CompressTask task = iterator.next();
            if (VideoCompressUtil.pathsEqual(task.inputPath, normalizedInputPath)) {
                iterator.remove();
                removed.add(task);
            }
        }
        return removed;
    }
}
