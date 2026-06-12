package com.hss01248.videocompress;

import android.app.Activity;
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

import com.hss01248.videocompress.listener.CompressLockReleaseListener;
import com.hss01248.videocompress.listener.CompressProgressDialogManager;
import com.hss01248.videocompress.listener.DefaultDialogCompressListener;
import com.hss01248.videocompress.listener.ICompressListener;
import com.hss01248.videocompress.listener.PostProcessorListener;
import com.hss01248.videocompress.listener.TerminalOnceListener;
import com.hss01248.videocompress.mediacodec.MediaCodecCompressImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class VideoCompressUtil {

   public static Context context ;
   public static boolean showCompressProgressDialog = false;
    public static boolean showCompareAfterCompress = AppUtils.isAppDebug();
    public static boolean showGridInfo = false;

    private static final CompressTaskQueue COMPRESS_QUEUE = new CompressTaskQueue();

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
        if (context instanceof Activity) {
            CompressProgressDialogManager.getInstance().bindHostActivity((Activity) context);
        }
    }
    public static void setCompressor(ICompressor compressor) {
        VideoCompressUtil.compressor = compressor;
    }

   static ICompressor compressor  = new MediaCodecCompressImpl();

    public static ICompressor getCompressor() {
        return compressor;
    }

    /** Whether a compress job is currently executing (not including queued-only tasks). */
    public static boolean isCompressing() {
        return COMPRESS_QUEUE.isRunning();
    }

    /** Running job + queued jobs waiting to start. */
    public static int getPendingTaskCount() {
        return COMPRESS_QUEUE.getPendingCount();
    }

    /**
     * Open the compress task list dialog (current and recent jobs).
     * Tasks appear in the list when {@link #showCompressProgressDialog} is enabled at compress time.
     */
    public static void showCompressTaskListDialog() {
        showCompressTaskListDialog(ActivityUtils.getTopActivity());
    }

    public static void showCompressTaskListDialog(Activity hostActivity) {
        CompressProgressDialogManager.getInstance().showTaskListDialog(hostActivity);
    }

    /**
     * Cancel pending (not yet started) compress jobs for {@code filePath}.
     * Jobs already executing are left to finish normally; this method returns false for them.
     *
     * @return true if at least one queued task was removed and {@link ICompressListener#onCancel()} delivered
     */
    public static boolean cancel(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        String normalized = normalizePath(filePath);
        List<CompressTask> removed = COMPRESS_QUEUE.removePendingByInputPath(normalized);
        if (removed.isEmpty()) {
            return false;
        }
        for (CompressTask task : removed) {
            deliverTaskCancelled(task);
        }
        return true;
    }

    static boolean pathsEqual(String a, String b) {
        return normalizePath(a).equals(normalizePath(b));
    }

    static String normalizePath(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            return new File(path).getAbsolutePath();
        }
    }

    private static void deliverTaskCancelled(CompressTask task) {
        if (task.progressTaskKey != null) {
            CompressProgressDialogManager.getInstance().onCancel(task.progressTaskKey);
        }
        task.listener.onCancel();
    }

    public static void doCompressAsync( String inputPath, @Nullable String outDir,
                                        @CompressType.Type String compressType,
                                        ICompressListener listener){
        doCompress(true,inputPath,outDir,compressType,listener);
    }

    public static void doCompress(boolean async,String inputPath, @Nullable String outDir,
                                  @CompressType.Type String compressType, ICompressListener listener){
        String progressTaskKey = null;
        if (VideoCompressUtil.showCompressProgressDialog) {
            progressTaskKey = CompressProgressDialogManager.getInstance().registerQueued(inputPath);
        }
        CompressTask task = new CompressTask(async, inputPath, outDir, compressType, listener, progressTaskKey);
        COMPRESS_QUEUE.enqueue(task, VideoCompressUtil::runNextQueuedTask);
    }

    private static void runNextQueuedTask() {
        CompressTask task = COMPRESS_QUEUE.pollNext();
        if (task == null) {
            COMPRESS_QUEUE.onTaskFinished(VideoCompressUtil::runNextQueuedTask);
            return;
        }
        doCompressInternal(task);
    }

    private static void doCompressInternal(CompressTask task) {
        Runnable advanceQueue = () -> COMPRESS_QUEUE.onTaskFinished(VideoCompressUtil::runNextQueuedTask);

        File input = new File(task.inputPath);
        File dir = new File(Utils.getApp().getExternalCacheDir(),"videoCompress");

        if(!TextUtils.isEmpty(task.outDir)){
            dir = new File(task.outDir);
        }
        dir.mkdirs();

        String fileName = input.getName();
        if(fileName.contains(".")){
           int idx =  fileName.lastIndexOf(".");
           fileName = fileName.substring(0,idx)+"-"+task.compressType+fileName.substring(idx);
        }
        File out = new File(dir,fileName);
        if(out.exists()){
            out.delete();
        }

        String outPath = out.getAbsolutePath();
        ICompressListener listener = task.listener;
        //装饰器模式:
        listener = new PostProcessorListener(listener);
        if(VideoCompressUtil.showCompressProgressDialog && task.progressTaskKey != null){
            listener = new DefaultDialogCompressListener(ActivityUtils.getTopActivity(), listener, task.progressTaskKey);
        }
        listener = new TerminalOnceListener(listener);
        listener = new CompressLockReleaseListener(listener, advanceQueue);

        try {
            out.createNewFile();
        } catch (IOException e) {
            Log.w("compress","createNewFile failed: " + outPath, e);
            listener.onError("createNewFile failed: " + e.getMessage());
            return;
        }

        listener.onStart(task.inputPath,outPath);

        VideoInfo.RealCompressInfo info = null;
        try {
            info = CompressHepler.getRealTargetWHBitrate(task.inputPath,task.compressType);
        } catch (Throwable e) {
            Log.i("compress","获取视频信息异常,跳过压缩,使用原文件: " + e.getMessage(),e);
            listener.onFinish(task.inputPath);
            return;
        }
        if(!info.needCompress){
            Log.i("compress","无需压缩: 实际比特率和分辨率小于期望比特率");
            MediaCodecCompressImpl.infoMap.put(task.inputPath,info);
            listener.onFinish(task.inputPath);
            return;
        }
        if(compressor instanceof MediaCodecCompressImpl){
            if("not_compact".equals(SPStaticUtils.getString("video_compress_mediacodec_compact"))){
                MediaCodecCompressImpl.setToUserFFmpeg();
            }
        }
        compressor.compress(task.async,info,task.inputPath,outPath,task.compressType,listener);
    }

}
