package com.hss01248.videocompress;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;


import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;

import java.io.File;
import java.io.FileInputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;



public class CompressHepler {



    /**
     * https://www.cnblogs.com/zhyan8/p/17233582.html
     * @param inputPath
     *
     * @return
     */
    public static VideoInfo.RealCompressInfo getRealTargetWHBitrate(String inputPath,  @CompressType.Type String compressType) throws Throwable{

        try {

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(inputPath);
            int originWidth = parseInt2(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH),"VIDEO_WIDTH");
            int originHeight = parseInt2(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT),"VIDEO_HEIGHT");
            int bitrate = parseInt2(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE),"KEY_BITRATE");

            //originWidth = 0;
            // originHeight =0;
            // bitrate =0;

            VideoInfo.RealCompressInfo info = new VideoInfo.RealCompressInfo();
            info.inputPath = inputPath;
            info.inputBitRate = bitrate;
            info.inputWidth = originWidth;
            info.inputHeight = originHeight;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //METADATA_KEY_VIDEO_FRAME_COUNT  关键帧总数
                //METADATA_KEY_CAPTURE_FRAMERATE 帧率
                info.inputFrameCount = parseInt2(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE),"CAPTURE_FRAMERATE");
                if(info.inputFrameCount == 0){
                    info.inputFrameCount = calFramCount(retriever);
                }
            }

            int targetResolution = CompressType.typeToResolution(compressType);
            calCompressConfig(info,targetResolution,originWidth,originHeight,bitrate,compressType);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                retriever.close();
            }
            return info;
        } catch (Throwable e) {
            throw e;
        }
    }

    public static int calFramCount( MediaMetadataRetriever retriever) {
        try {
            //获取视频帧数
            String count_s = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                count_s = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
                long count = Long.parseLong(count_s);
//计算帧率
                //获取视频时长，单位：毫秒(ms)
                String duration_s = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long duration = Long.parseLong(duration_s);
                long dt = Math.round(count*1000.0/duration);
                return  (int) dt;
            }
        }catch (Throwable throwable){
            LogUtils.d(throwable);
        }
        return 0;
    }

    private static int parseInt2(String s,String desc) {
        if(TextUtils.isEmpty(s)){
            LogUtils.w("value is empty",desc);
            return 0;
        }
        try {
            return Integer.parseInt(s);
        }catch (Throwable throwable){
            LogUtils.w(s,throwable);
        }
        return 0;
    }

    /**
     * 码率/分辨率 比例,线性拟合
     *  720p上传的 拟合数据源: 阿里云点播码率表  https://help.aliyun.com/document_detail/86068.html  y = 0.0018x - 545.63
     *
     *  本地收藏保存: y = 0.0018x + 1059.6
     * @param originWidth
     * @param originHeight
     * @return kbps
     *
     *  国内阿里和b站的都抠抠搜搜,推荐使用youtube的码率表:
     * 24帧/30帧:
     * 1080p 10 Mbps
     * 720p	 6.5 Mbps
     * https://wangwei1237.github.io/2021/05/28/Recommended-video-bitrates-for-different-resolutions/
     */
    public static int getExpectedBitRate(int originWidth, int originHeight, @CompressType.Type String compressType) {
        return VideoCompressUtil.getGlobalBitRateConfig().getExpectedBitRate(compressType);
    }

    /**
     *
     * @param targetResolution
     * @param inputWidth
     * @param inputHeight
     * @param originalBitrate
     * @param compressType
     * @return 返回是否需要压缩
     */
    private static boolean calCompressConfig(VideoInfo.RealCompressInfo info, int targetResolution,
                                      int inputWidth, int inputHeight, int originalBitrate, String compressType) {
        int sourceResolution = Math.min(inputWidth,inputHeight);
        if(sourceResolution <= targetResolution){

            compressType = CompressType.resolutionToType(sourceResolution);
            int expetedRatesInkps = VideoCompressUtil.getGlobalBitRateConfig().getExpectedBitRate(compressType);
            LogUtils.d("不需要压缩尺寸,只需要压缩码率,比较码率:",originalBitrate/8,expetedRatesInkps/8);
            if(originalBitrate <= expetedRatesInkps){
                LogUtils.d("原始码率和尺寸均小于目标码率尺寸,无需压缩");
                info.desc = "原始码率和尺寸均小于目标码率尺寸,无需压缩";
                info.needCompress = false;
                return false;
            }else {
                LogUtils.d("尺寸不需要压缩,但需要压缩码率");
                info.desc = "尺寸不需要压缩,但需要压缩码率:"+expetedRatesInkps/8/1024/1024.0f+"MB/s";
                info.outWidth = inputWidth;
                info.outHeight = inputHeight;
                info.outBitRate = expetedRatesInkps;
            }
        }else {

            int expetedRatesInkps = VideoCompressUtil.getGlobalBitRateConfig().getExpectedBitRate(compressType);
            LogUtils.d("需要压缩尺寸+码率: 尺寸从大往小压,码率也是从大往小",sourceResolution+"p -> "
                    +targetResolution+"p,originalBitrate:"+originalBitrate+",expetedRatesInkps:"+expetedRatesInkps);
            info.desc = "需要压缩尺寸+码率: 尺寸从大往小压,码率也是从大往小:"+originalBitrate+","+expetedRatesInkps/8/1024/1024.0f+"MB/s";
            if(originalBitrate < expetedRatesInkps){
                info.desc = "原始尺寸更大,但码率却更小,那么使用原始码率";
                LogUtils.i("原始尺寸更大,但码率却更小,那么使用原始码率,",originalBitrate,expetedRatesInkps);
            }
            info.outBitRate = Math.min(originalBitrate,expetedRatesInkps);
            float rate = 0;
            if(inputWidth < inputHeight){
                //rate = inputWidth*1.0f/targetResolution;
                int targetHeight = Math.round(targetResolution*inputHeight*1.0f/inputWidth);
                info.outWidth = targetResolution;
                info.outHeight = targetHeight;
            }else {
                //rate = inputHeight*1.0f/targetResolution;
                int targetW = Math.round(targetResolution*inputWidth*1.0f/inputHeight);
                info.outWidth = targetW;
                info.outHeight = targetResolution;
            }
            //实际传入码率需要除以尺寸的倍率:也不对,码率不稳定
           // LogUtils.i("实际传入码率需要除以尺寸的倍率:",info.outBitRate,rate,"最终码率:"+Math.round(info.outBitRate/rate));
            //info.outBitRate = Math.round(info.outBitRate/rate);
        }
        info.needCompress = true;
        return true;
    }

    private static int getBitRate(int expetedRatesInkps, int originalBitrate, float ratio) {
        return Math.min(expetedRatesInkps,originalBitrate);
    }



    public static  void refreshMediaCenter(Context activity, String filePath){
        if (Build.VERSION.SDK_INT>19){
            String mineType =getMineType(filePath);

            saveImageSendScanner(activity,new MyMediaScannerConnectionClient(filePath,mineType));
        }else {

            saveImageSendBroadcast(activity,filePath);
        }
    }

    public static String getMineType(String filePath) {

        String type = "text/plain";
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;


       /* MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String mime = "text/plain";
        if (filePath != null) {
            try {
                mmr.setDataSource(filePath);
                mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            } catch (IllegalStateException e) {
                return mime;
            } catch (IllegalArgumentException e) {
                return mime;
            } catch (RuntimeException e) {
                return mime;
            }
        }
        return mime;*/
    }

    /**
     * 保存后用广播扫描，Android4.4以下使用这个方法
     * @author YOLANDA
     */
    private static void saveImageSendBroadcast(Context activity, String filePath){
        activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filePath)));
    }

    /**
     * 保存后用MediaScanner扫描，通用的方法
     *
     */
    private static void saveImageSendScanner (Context context, MyMediaScannerConnectionClient scannerClient) {

        final MediaScannerConnection scanner = new MediaScannerConnection(context, scannerClient);
        scannerClient.setScanner(scanner);
        scanner.connect();
    }
    private   static class MyMediaScannerConnectionClient implements MediaScannerConnection.MediaScannerConnectionClient {

        private MediaScannerConnection mScanner;

        private String mScanPath;
        private String mimeType;

        public MyMediaScannerConnectionClient(String scanPath, String mimeType) {
            mScanPath = scanPath;
            this.mimeType = mimeType;
        }

        public void setScanner(MediaScannerConnection con) {
            mScanner = con;
        }

        @Override
        public void onMediaScannerConnected() {
            mScanner.scanFile(mScanPath, mimeType);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            mScanner.disconnect();
        }
    }

    public static void copyFile(@NonNull String pathFrom, @NonNull String pathTo) throws IOException {
        if (pathFrom.equalsIgnoreCase(pathTo)) {
            return;
        }

        FileChannel outputChannel = null;
        FileChannel inputChannel = null;
        try {
            inputChannel = new FileInputStream(new File(pathFrom)).getChannel();
            outputChannel = new FileOutputStream(new File(pathTo)).getChannel();
            inputChannel.transferTo(0, inputChannel.size(), outputChannel);
            inputChannel.close();
        } finally {
            if (inputChannel != null) inputChannel.close();
            if (outputChannel != null) outputChannel.close();
        }
    }
}
