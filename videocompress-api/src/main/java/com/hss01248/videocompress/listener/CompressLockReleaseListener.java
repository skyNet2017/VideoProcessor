package com.hss01248.videocompress.listener;

/**
 * Invokes a session-end hook when a terminal callback is forwarded (e.g. advance compress queue).
 */
public class CompressLockReleaseListener implements ICompressListener {

    private final ICompressListener delegate;
    private final Runnable onSessionEnd;

    public CompressLockReleaseListener(ICompressListener delegate, Runnable onSessionEnd) {
        this.delegate = delegate;
        this.onSessionEnd = onSessionEnd;
    }

    private void endSession() {
        onSessionEnd.run();
    }

    @Override
    public void onStart(String inputPath, String outPath) {
        delegate.onStart(inputPath, outPath);
    }

    @Override
    public void onProgress(int progress, long progressTime) {
        delegate.onProgress(progress, progressTime);
    }

    @Override
    public void onFinish(String outputFilePath) {
        try {
            delegate.onFinish(outputFilePath);
        } finally {
            endSession();
        }
    }

    @Override
    public void onError(String message) {
        try {
            delegate.onError(message);
        } finally {
            endSession();
        }
    }

    @Override
    public void onCancel() {
        try {
            delegate.onCancel();
        } finally {
            endSession();
        }
    }
}
