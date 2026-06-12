package com.hss01248.videocompress.listener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ensures at most one terminal callback ({@link #onFinish}, {@link #onError}, {@link #onCancel})
 * is forwarded. Progress updates are ignored after a terminal state is reached.
 */
public class TerminalOnceListener implements ICompressListener {

    private static final int RUNNING = 0;
    private static final int FINISHED = 1;
    private static final int ERROR = 2;
    private static final int CANCELLED = 3;

    private final ICompressListener delegate;
    private final AtomicInteger state = new AtomicInteger(RUNNING);

    public TerminalOnceListener(ICompressListener delegate) {
        this.delegate = delegate;
    }

    public boolean isTerminal() {
        return state.get() != RUNNING;
    }

    @Override
    public void onStart(String inputPath, String outPath) {
        delegate.onStart(inputPath, outPath);
    }

    @Override
    public void onProgress(int progress, long progressTime) {
        if (state.get() == RUNNING) {
            delegate.onProgress(progress, progressTime);
        }
    }

    @Override
    public void onFinish(String outputFilePath) {
        if (state.compareAndSet(RUNNING, FINISHED)) {
            delegate.onFinish(outputFilePath);
        }
    }

    @Override
    public void onError(String message) {
        if (state.compareAndSet(RUNNING, ERROR)) {
            delegate.onError(message);
        }
    }

    @Override
    public void onCancel() {
        if (state.compareAndSet(RUNNING, CANCELLED)) {
            delegate.onCancel();
        }
    }
}
