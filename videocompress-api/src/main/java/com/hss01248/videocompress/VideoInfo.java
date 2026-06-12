package com.hss01248.videocompress;

import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;


import com.blankj.utilcode.util.LogUtils;
import com.hss01248.videocompress.mediacodec.MediaCodecCompressImpl;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;


@Keep
public class VideoInfo {
    public String name;
    public String path;
    public int height;
    public int width;
    public int rotation;
    public long fileLength;
    public float duration;
    public int bitRates;
    public int framePs;
    public String errMsg;
    public String desc;
    public int quality;
    public Map<String,String> info;


    public static VideoInfo getInfo(String path) {
        VideoInfo info = new VideoInfo();
        info.path = path;
        File file = new File(path);
        try{
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(path);
            info.width = toInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)); //宽
            info.height = toInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)); //高
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                info.rotation = toInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));//视频的方向角度
            }
            info.duration = toInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000.0f;//视频的长度 s
            info.bitRates = toInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)) / 1024; //kbps 按字节计算. 不按比特
            if (info.bitRates <= 0 && info.duration > 0) {
                info.bitRates = (int) (file.length() * 8 / info.duration) / 1024 ;
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                info.framePs = toInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE));
                if(info.framePs ==0){
                    info.framePs = CompressHepler.calFramCount(retriever);
                }
            }
        }catch (Throwable throwable){
            LogUtils.w(throwable);
            info.errMsg = throwable.getMessage();
        }

        info.name = file.getName();
        info.fileLength = file.length();
        if(MediaCodecCompressImpl.infoMap.get(path) !=null){
            info.desc = MediaCodecCompressImpl.infoMap.get(path).desc;
        }


      // info.info =  getAllInfo(path);
        return info;
    }

    @Override
    public String toString() {
        return
                "wh=" + width +
                        "x" + height +
                        "\nrotation=" + rotation +
                        "\nfileLength=" + size(fileLength) +
                        "\nduration=" + String.format("%.2f",duration) + "s" +
                        "\nbitRates=" + bitRates + "kbps" +
                        "\nbitRates(B)=" + String.format("%.2f",bitRates/1024.0/8) + "MB/s" +
                        "\nfps=" +framePs +
                        "\nfile=" +name +
                        "\npath=" + path+
                        "\n\ndesc=" + desc+
        "\nerrMsg=" + errMsg;
    }

    public String showAllInfo(){
        return getAllInfo(path).toString();
    }

    private static String size(long len) {
        String size = "";
        if (len > 1024 * 1024) {
            size = len / 1024 / 1024 + "MB";
        } else {
            size = len / 1024 + "kB";
        }
        return size;
    }

    public static Map<String,String> getAllInfo(String path){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
       Field[] fields = MediaMetadataRetriever.class.getDeclaredFields();
       Map<String,String> metadatas = new TreeMap<>();
       String key = "METADATA_KEY_";
       try {
           for (Field field : fields) {
               if(  Modifier.isFinal(field.getModifiers())  && Modifier.isStatic(field.getModifiers())){
                   String name = field.getName();
                   field.setAccessible(true);
                   if(name.startsWith(key)){
                     Object o =  field.get(MediaMetadataRetriever.class);
                       //Log.d("media","field:"+name+" v:"+o);
                     if(o instanceof Integer){
                         int keyCode = (int) o;
                         String s = retriever.extractMetadata(keyCode);
                         String key2 = name.substring(key.length()).toLowerCase();
                         //Log.d("media",key2+" :"+s);
                         if(!TextUtils.isEmpty(s)){
                             metadatas.put(key2,s);
                             Log.d("mediainfo",key2+" :"+s);
                         }

                     }

                   }
               }
           }
       }catch (Throwable throwable){
           throwable.printStackTrace();
       }
       return metadatas;
    }

    public String getInfoForList(){
        File file = new File(path);
        StringBuilder stringBuilder = new StringBuilder();
       String  bitRate = "";
       if(bitRates>1024){
           bitRate = String.format("%.1fMbps",bitRates/1024f);
       }else {
           bitRate = bitRates +"kbps";
       }
        String  byteRate = "";
        if(bitRates/8>1024){
            byteRate = String.format("%.1fMBps",bitRates/1024f/8);
        }else {
            byteRate = bitRates/8 +"kBps";
        }
        stringBuilder//.append(file.getName())
               // .append("\n")
                .append(size(file.length()))
                .append("\n")
                .append(width)
                .append("x")
                .append(height)
                .append(" r:")
                .append(rotation)
                .append("\n")
                .append("bit_rate:")
                .append(bitRate)
                .append("\n")
                .append("byte_rate:")
                .append(byteRate)
                .append("\n")
                .append("len:")
                .append(String.format("%.2f",duration))  .append("s") ;
        return stringBuilder.toString();
    }

    public static int toInt(Object o, int defaultValue) {
        if (o == null) {
            return defaultValue;
        }
        int value;
        try {
            String s = o.toString().trim();
            if (s.contains(".")) {
                value = Integer.valueOf(s.substring(0, s.lastIndexOf(".")));
            } else {
                value = Integer.valueOf(s);
            }
        } catch (Exception e) {
            LogUtils.w(e);
            value = defaultValue;
        }

        return value;
    }

    public static int toInt(Object o) {
        return toInt(o, 0);
    }






    @Keep
    public static class RealCompressInfo{
        public String inputPath;
        public String outPath;

        public int inputHeight;
        public int inputWidth;
        public int inputBitRate;
        public int inputFrameCount;


        public int outHeight;
        public int outWidth;
        public int outBitRate;
        public int outFrameCount;
        public int outRsf;
        public String desc;
        public String uriPath;

        public boolean needCompress = true;

    }

}
