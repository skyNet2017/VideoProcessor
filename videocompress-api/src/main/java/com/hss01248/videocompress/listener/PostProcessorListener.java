package com.hss01248.videocompress.listener;

import android.util.Log;

import java.io.File;

public class PostProcessorListener implements ICompressListener{
    ICompressListener listener;
    static String TAG = "compressor";

    String inputPath;
    public PostProcessorListener( ICompressListener listener) {
        this.listener = listener;
    }

    @Override
    public void onStart(String inputPath,String outPath) {
        this.inputPath = inputPath;
        listener.onStart(inputPath, outPath);

    }

    private void logd(String s) {
        Log.w(TAG,s);
    }

    @Override
    public void onProgress(int progress, long progressTime) {
        listener.onProgress(progress, progressTime);
    }

    @Override
    public void onError(String message) {
        listener.onError(message);

    }

    @Override
    public void onFinish(String outputFilePath) {
        if(inputPath != null && outputFilePath != null) {
            File input = new File(inputPath);
            File output = new File(outputFilePath);
            if(output.exists() && input.exists() && output.length() > input.length()){
                output.delete();
                outputFilePath = inputPath;
                logd("压缩后文件变大,舍弃掉压缩文件,使用原文件");
            }
        }
        listener.onFinish(outputFilePath);
    }
}
