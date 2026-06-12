package com.hss01248.videocompress.listener;

import android.app.Activity;

import com.blankj.utilcode.util.LogUtils;

public class DefaultDialogCompressListener implements ICompressListener {

    private final String taskKey;
    private final ICompressListener listener;

    public DefaultDialogCompressListener(Activity activity, ICompressListener listener, String taskKey) {
        this.listener = listener;
        this.taskKey = taskKey;
        CompressProgressDialogManager.getInstance().bindHostActivity(activity);
    }

    @Override
    public void onStart(String inputPath, String outPath) {
        listener.onStart(inputPath, outPath);
        try {
            CompressProgressDialogManager.getInstance().onStart(taskKey, inputPath, outPath);
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }
    }

    @Override
    public void onProgress(int progress, long progressTime) {
        listener.onProgress(progress, progressTime);
        try {
            CompressProgressDialogManager.getInstance().onProgress(taskKey, progress);
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }
    }

    @Override
    public void onError(String message) {
        listener.onError(message);
        try {
            CompressProgressDialogManager.getInstance().onError(taskKey, message);
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }
    }

    @Override
    public void onFinish(String outputFilePath) {
        listener.onFinish(outputFilePath);
        try {
            CompressProgressDialogManager.getInstance().onFinish(taskKey, outputFilePath);
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }
    }

    @Override
    public void onCancel() {
        listener.onCancel();
        try {
            CompressProgressDialogManager.getInstance().onCancel(taskKey);
        } catch (Throwable throwable) {
            LogUtils.w(throwable);
        }
    }
}
