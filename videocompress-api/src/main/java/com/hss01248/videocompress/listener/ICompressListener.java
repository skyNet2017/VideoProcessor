package com.hss01248.videocompress.listener;

public interface ICompressListener {

    default void onStart(String inputPath,String outPath){}

    /**
     * 执行完成
     */
    void onFinish(String outputFilePath);

    /**
     * 进度回调
     *
     * @param progress     执行进度
     * @param progressTime 执行的时间，相对于总时间 单位：微秒
     */
   default void onProgress(int progress, long progressTime){}

    /**
     * 执行取消
     */
   default void onCancel(){}

    /**
     * 执行出错
     *
     * @param message
     */
    void onError(String message);
}
