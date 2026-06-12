package com.hss01248.videocompress.listener;

import android.util.Log;

import com.hss01248.videocompress.VideoInfo;


public class CompressLogListener implements ICompressListener {
    ICompressListener listener;
    static String TAG = "compressor";

    public CompressLogListener( ICompressListener listener) {
        this.listener = listener;
    }

    long start ;
    @Override
    public void onStart(String inputPath,String outPath) {
        listener.onStart(inputPath, outPath);
        logd("onstart: input:"+inputPath+"\nout:"+outPath);
        start = System.currentTimeMillis();

    }

    private void logd(String s) {
        Log.d(TAG,s);
    }

    @Override
    public void onProgress(int progress, long progressTime) {
        listener.onProgress(progress, progressTime);
        logd("progress:"+progress+" , time cost:"+progressTime/1000+"s");

    }

    @Override
    public void onError(String message) {
        listener.onError(message);
        logd("onError:"+message+" , time cost:"+(System.currentTimeMillis() - start)/1000+"s");

    }

    @Override
    public void onFinish(String outputFilePath) {
        logd("onFinish:"+" , time cost:"+(System.currentTimeMillis() - start)/1000+"s \nout path:"+outputFilePath);
        VideoInfo.getAllInfo(outputFilePath);
        listener.onFinish(outputFilePath);

    }

}
