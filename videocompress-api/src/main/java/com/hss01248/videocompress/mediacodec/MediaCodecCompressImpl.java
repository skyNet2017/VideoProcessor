package com.hss01248.videocompress.mediacodec;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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

        ICompressListener  listener = new ICompressListener() {
            @Override
            public void onFinish(String outputFilePath) {
                //检查压缩后的文件是否有效,如果无效,则采用ffmpeg的压缩方式;
                // 比较大的可能是文件头最后写,还没有写完,所以延迟1.5s
                ThreadUtils.getMainHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            LogUtils.d("----------> 0.5s after onFinished() called, check and call real onfinished() ");
                            File file = new File(outputFilePath);
                            if(!file.exists() || file.length() ==0){
                                listener0.onError("compress failed: file length is 0");
                                return;
                            }
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(outputFilePath);
                            String originWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                            String originHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                            // int rotationValue = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
                            // int oriBitrate = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
                            String durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                            retriever.release();
                            if(originWidth == null && originHeight ==null && durationMs ==null){
                                //不兼容硬件压缩,需要使用ffmepg压缩方式:
                                SPStaticUtils.put("video_compress_mediacodec_compact","not_compact");
                                setToUserFFmpeg();
                                listener0.onError("compress failed: not compact with media codec compressor, please retry");
                            }else {
                                infoMap.put(outputFilePath,info);
                                listener0.onFinish(outputFilePath);
                            }
                        } catch (Exception e) {
                            LogUtils.e(outputFilePath,e);
                            listener0.onError(e.getClass().getSimpleName()+" : "+e.getMessage());
                        }
                    }
                },500);
            }

            @Override
            public void onError(String message) {
                listener0.onError(message);
            }

            @Override
            public void onProgress(int progress, long progressTime) {
                ICompressListener.super.onProgress(progress, progressTime);
                listener0.onProgress(progress, progressTime);
            }

            @Override
            public void onCancel() {
                listener0.onCancel();

            }
        };

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    //VideoInfo info = VideoInfo.getInfo(inputPath);
                    long start = System.currentTimeMillis();


                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
                        //兼容性处理,api21以下,不压缩.
                        listener.onFinish(inputPath);
                        return;
                    }

                    int frameCount = info.inputFrameCount;
                    if(frameCount > 30 || frameCount ==0){
                        LogUtils.i("视频帧率>30或=0,则设置为30:"+inputPath+", "+frameCount);
                        frameCount = 30;
                    }

                    VideoProcessor.Processor processor = VideoProcessor.processor(Utils.getApp())
                            //给activity添加硬件加速后压缩效率和时间会提高很多//打开了还是一样呢
                            .input(inputPath)
                            .output(outPath)
                            .outWidth(info.outWidth)
                            .outHeight(info.outHeight)
                            .bitrate(info.outBitRate)
                            //压缩视频码率设置跟最终生成的不一致:
                            // 压缩的时候没有设置帧率，你那默认是用的30而不是读取视频的真实帧率
                            //如果帧率设置为视频的真实帧率，码率就一致了
                            //不能低码率往高码率转
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
                        listener0.onError("UncaughtException: " + e.getMessage());
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
