package com.hss01248.videocompress.listener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.TouchUtils;
import com.hss01248.videocompress.R;
import com.hss01248.videocompress.VideoCompressUtil;
import com.hss01248.videocompress.compare.CompressCompareActivity;
import com.hss01248.videocompress.listener.ICompressListener;

import java.io.File;

public class DefaultDialogCompressListener implements ICompressListener {
   protected Handler handler  = new Handler(Looper.getMainLooper());
    protected final ProgressDialog[] dialog = {null};
    protected ICompressListener listener;

    public DefaultDialogCompressListener(Activity activity,ICompressListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

   protected Activity activity;
    protected String inputPath;
    protected long start;
    @Override
    public void onStart(String inputPath,String outPath) {
        listener.onStart(inputPath, outPath);
        this.inputPath = inputPath;
        start = System.currentTimeMillis();
        handler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    if(activity.isDestroyed() || activity.isFinishing()){
                        activity = ActivityUtils.getTopActivity();
                    }
                    if(activity.isDestroyed() || activity.isFinishing()){
                        activity = ActivityUtils.getTopActivity();
                    }
                    dialog[0] = new ProgressDialog(activity);
                    dialog[0].setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    dialog[0].setTitle(activity.getString(R.string.vc_compressing)+": "+new File(inputPath).getName());
                    dialog[0].setMax(100);
                    dialog[0].setCancelable(false);
                    dialog[0].setCanceledOnTouchOutside(false);
                    dialog[0].show();
                }catch (Throwable throwable){
                    LogUtils.w(throwable);
                }

            }
        });
    }

    @Override
    public void onProgress(int progress, long progressTime) {
        listener.onProgress(progress, progressTime);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    if(dialog[0] != null && dialog[0].isShowing()){
                        dialog[0].setProgress(progress);
                    }
                }catch (Throwable throwable){
                    LogUtils.w(throwable);
                }
            }
        });
    }

    @Override
    public void onError(String message) {
        listener.onError(message);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    if(dialog[0] != null && dialog[0].isShowing()){
                        dialog[0].dismiss();
                    }
                    ToastUtils.showShort(message);
                }catch (Throwable throwable){
                    LogUtils.w(throwable);
                }
            }
        });
    }

    @Override
    public void onFinish(String outputFilePath) {
        listener.onFinish(outputFilePath);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    if(dialog[0] != null && dialog[0].isShowing()){
                        dialog[0].dismiss();
                    }
                }catch (Throwable throwable){
                    LogUtils.w(throwable);
                }
                if(VideoCompressUtil.showCompareAfterCompress){
                    CompressCompareActivity.start(activity,inputPath,outputFilePath,start);
                }
                //showInfo(file,out,start,activity,handler);
            }
        });
    }
}
