package com.hss01248.videocompress;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.SPStaticUtils;
import com.blankj.utilcode.util.Utils;
import com.hss01248.videocompress.bitrate.LowThanBiliBitrateConfig;
import com.hss01248.videocompress.bitrate.IBitrateConfig;

import com.hss01248.videocompress.listener.DefaultDialogCompressListener;
import com.hss01248.videocompress.listener.ICompressListener;
import com.hss01248.videocompress.listener.PostProcessorListener;
import com.hss01248.videocompress.mediacodec.MediaCodecCompressImpl;

import java.io.File;
import java.io.IOException;


public class VideoCompressUtil {

   public static Context context ;
   public static boolean showCompressProgressDialog = false;
    public static boolean showCompareAfterCompress = AppUtils.isAppDebug();
    public static boolean showGridInfo = false;

    public static void setGlobalBitRateConfig(IBitrateConfig globalBitRateConfig) {
        VideoCompressUtil.globalBitRateConfig = globalBitRateConfig;
    }

    public static IBitrateConfig getGlobalBitRateConfig() {
        return globalBitRateConfig;
    }

    static  IBitrateConfig globalBitRateConfig = new LowThanBiliBitrateConfig();

    public static void setiPreviewVideo(IPreviewVideo iPreviewVideo) {
        VideoCompressUtil.iPreviewVideo = iPreviewVideo;
    }

    public static IPreviewVideo iPreviewVideo;

    public static void init(Context context,boolean showCompressProgressDialog,
                            boolean showCompareAfterCompress){
        VideoCompressUtil.context = Utils.getApp();
        VideoCompressUtil.showCompressProgressDialog = showCompressProgressDialog;
        VideoCompressUtil.showCompareAfterCompress = showCompareAfterCompress;
    }
    public static void setCompressor(ICompressor compressor) {
        VideoCompressUtil.compressor = compressor;
    }

   static ICompressor compressor  = new MediaCodecCompressImpl();

    public static void doCompressAsync( String inputPath, @Nullable String outDir,
                                        @CompressType.Type String compressType,
                                        ICompressListener listener){
        doCompress(true,inputPath,outDir,compressType,listener);
    }

    public static void doCompress(boolean async,String inputPath, @Nullable String outDir,
                                  @CompressType.Type String compressType, ICompressListener listener){

        File input = new File(inputPath);
        File dir = new File(Utils.getApp().getExternalCacheDir(),"videoCompress");

        if(!TextUtils.isEmpty(outDir)){
            dir = new File(outDir);
        }
        dir.mkdirs();

        String fileName = input.getName();
        if(fileName.contains(".")){
           int idx =  fileName.lastIndexOf(".");
           fileName = fileName.substring(0,idx)+"-"+compressType+fileName.substring(idx);
        }
        File out = new File(dir,fileName);
        if(out.exists()){
            out.delete();
        }

        String outPath = out.getAbsolutePath();
        //装饰器模式:
        listener = new PostProcessorListener(listener);
        if(VideoCompressUtil.showCompressProgressDialog){
            listener = new DefaultDialogCompressListener(ActivityUtils.getTopActivity(),listener);
        }

        try {
            out.createNewFile();
        } catch (IOException e) {
            Log.w("compress","createNewFile failed: " + outPath, e);
            listener.onError("createNewFile failed: " + e.getMessage());
            return;
        }

        listener.onStart(inputPath,outPath);

        VideoInfo.RealCompressInfo info = null;
        try {
            info = CompressHepler.getRealTargetWHBitrate(inputPath,compressType);
        } catch (Throwable e) {
            Log.i("compress","获取视频信息异常,跳过压缩,使用原文件: " + e.getMessage(),e);
            listener.onFinish(inputPath);
            return;
        }
        if(!info.needCompress){
            Log.i("compress","无需压缩: 实际比特率和分辨率小于期望比特率");
            //无需压缩
            MediaCodecCompressImpl.infoMap.put(inputPath,info);
            listener.onFinish(inputPath);
            return;
        }
        if(compressor instanceof MediaCodecCompressImpl){
            if("not_compact".equals(SPStaticUtils.getString("video_compress_mediacodec_compact"))){
                MediaCodecCompressImpl.setToUserFFmpeg();
            }
        }
        compressor.compress(async,info,inputPath,outPath,compressType,listener);

    }



}
