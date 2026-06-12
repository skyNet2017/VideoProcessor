package com.hss01248.videocompress.mediacodec;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;


import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPStaticUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.videocompress.CompressType;
import com.hss01248.videocompress.VideoCompressUtil;
import com.hss01248.videocompress.VideoInfo;
import com.hss01248.videocompress.listener.ICompressListener;
import com.hss01248.videocompress.ICompressor;
import com.hw.videoprocessor.VideoProcessor;
import com.hw.videoprocessor.util.VideoProgressListener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaCodecCompressImpl implements ICompressor {
    /**
     * https://github.com/yellowcath/VideoProcessor
     * VideoProcessor使用Android原生的MediaCodec实现视频压缩、剪辑、混音、快慢放及倒流的功能（快慢放及倒流支持音频同步变化），在支持MediaCodec的手机上优于使用FFmpeg的方案
     *
     * 体积小 ：编译后的aar只有262K，ffmpeg一个so就7、8M，精简之后也差不多还有一半大小
     * 速度快 ：在huaweiP9上压缩(1080P 20s 20000k -> 720p 2000k)
     * @param inputPath
     * @param outPath
     * @param compressType
     * @param listener
     */
    @SuppressWarnings("AlibabaAvoidManuallyCreateThread")
    @Override
    public void compress(boolean async,VideoInfo.RealCompressInfo info ,String inputPath, String outPath, @CompressType.Type String compressType,
                         ICompressListener listener0) {

        final Handler mainHandler = ThreadUtils.getMainHandler();
        final AtomicBoolean terminalDelivered = new AtomicBoolean(false);
        final Runnable[] pendingFinishCheck = new Runnable[1];

        ICompressListener listener = new ICompressListener() {
            private void cancelPendingFinishCheck() {
                if (pendingFinishCheck[0] != null) {
                    mainHandler.removeCallbacks(pendingFinishCheck[0]);
                    pendingFinishCheck[0] = null;
                }
            }

            private void deliverFinish(String outputFilePath) {
                cancelPendingFinishCheck();
                if (terminalDelivered.compareAndSet(false, true)) {
                    listener0.onFinish(outputFilePath);
                }
            }

            private void deliverError(String message) {
                cancelPendingFinishCheck();
                if (terminalDelivered.compareAndSet(false, true)) {
                    listener0.onError(message);
                }
            }

            private void retryWithFfmpeg() {
                cancelPendingFinishCheck();
                if (terminalDelivered.get()) {
                    return;
                }
                SPStaticUtils.put("video_compress_mediacodec_compact", "not_compact");
                setToUserFFmpeg();
                ICompressor ffmpegCompressor = VideoCompressUtil.getCompressor();
                if (ffmpegCompressor == null || ffmpegCompressor instanceof MediaCodecCompressImpl) {
                    deliverError("compress failed: ffmpeg fallback unavailable");
                    return;
                }
                LogUtils.i("MediaCodec incompatible, retry with FFmpeg: " + inputPath);
                ffmpegCompressor.compress(async, info, inputPath, outPath, compressType, new ICompressListener() {
                    @Override
                    public void onFinish(String outputFilePath) {
                        infoMap.put(outputFilePath, info);
                        deliverFinish(outputFilePath);
                    }

                    @Override
                    public void onError(String message) {
                        deliverError(message);
                    }

                    @Override
                    public void onProgress(int progress, long progressTime) {
                        if (!terminalDelivered.get()) {
                            listener0.onProgress(progress, progressTime);
                        }
                    }

                    @Override
                    public void onCancel() {
                        cancelPendingFinishCheck();
                        if (terminalDelivered.compareAndSet(false, true)) {
                            listener0.onCancel();
                        }
                    }
                });
            }

            @Override
            public void onFinish(String outputFilePath) {
                cancelPendingFinishCheck();
                pendingFinishCheck[0] = new Runnable() {
                    @Override
                    public void run() {
                        pendingFinishCheck[0] = null;
                        if (terminalDelivered.get()) {
                            return;
                        }
                        try {
                            LogUtils.d("----------> 0.5s after onFinished() called, check and call real onfinished() ");
                            File file = new File(outputFilePath);
                            if (!file.exists() || file.length() == 0) {
                                deliverError("compress failed: file length is 0");
                                return;
                            }
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(outputFilePath);
                            String originWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                            String originHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                            String durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            retriever.release();
                            if (originWidth == null && originHeight == null && durationMs == null) {
                                retryWithFfmpeg();
                            } else {
                                infoMap.put(outputFilePath, info);
                                deliverFinish(outputFilePath);
                            }
                        } catch (Exception e) {
                            LogUtils.e(outputFilePath, e);
                            deliverError(e.getClass().getSimpleName() + " : " + e.getMessage());
                        }
                    }
                };
                mainHandler.postDelayed(pendingFinishCheck[0], 500);
            }

            @Override
            public void onError(String message) {
                deliverError(message);
            }

            @Override
            public void onProgress(int progress, long progressTime) {
                if (!terminalDelivered.get()) {
                    listener0.onProgress(progress, progressTime);
                }
            }

            @Override
            public void onCancel() {
                cancelPendingFinishCheck();
                if (terminalDelivered.compareAndSet(false, true)) {
                    listener0.onCancel();
                }
            }
        };

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    long start = System.currentTimeMillis();

                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                        listener.onFinish(inputPath);
                        return;
                    }

                    int frameCount = info.inputFrameCount;
                    if(frameCount > 30 || frameCount ==0){
                        LogUtils.i("视频帧率>30或=0,则设置为30:"+inputPath+", "+frameCount);
                        frameCount = 30;
                    }

                    VideoProcessor.Processor processor = VideoProcessor.processor(Utils.getApp())
                            .input(inputPath)
                            .output(outPath)
                            .outWidth(info.outWidth)
                            .outHeight(info.outHeight)
                            .bitrate(info.outBitRate)
                            .frameRate(frameCount)
                            .progressListener(new VideoProgressListener() {
                                @Override
                                public void onProgress(float progress) {
                                    listener.onProgress((int) (progress * 100), System.currentTimeMillis() - start);
                                    if (progress == 1.0f) {
                                        LogUtils.d("----------> progress == 1.0f callback ");
                                    }
                                }
                            });
                    processor.process();
                    LogUtils.d("----------> after process() call");
                    listener.onFinish(outPath);
                } catch (Throwable e) {
                    LogUtils.w(e,inputPath);
                    listener.onError(e.getClass().getName()+": "+e.getMessage());
                }
            }
        };
        if(async){
            Thread thread = new Thread(runnable, "VideoCompress");
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LogUtils.e("VideoCompress thread uncaught exception", e);
                    try {
                        listener.onError("UncaughtException: " + e.getMessage());
                    } catch (Throwable ignored) {}
                }
            });
            thread.start();
        }else {
            runnable.run();
        }
    }

    public static Map<String,VideoInfo.RealCompressInfo> infoMap = new HashMap<>();
    public static Map<String, Uri> uriMap = new HashMap<>();

   public static void setToUserFFmpeg() {
        String className = "com.hss01248.base.compressorimpl.FFmpegCompressImpl";
       try {
           Class clazz = Class.forName(className);
           Object instance = clazz.newInstance();
           VideoCompressUtil.setCompressor((ICompressor) instance);
       } catch (Exception e) {
           LogUtils.e(e);
       }
    }


}
