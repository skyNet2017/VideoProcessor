package com.hss01248.base.compressorimpl;

import android.text.TextUtils;

import com.hss01248.videocompress.CompressorConfig;
import com.hss01248.videocompress.VideoInfo;

import io.microshow.rxffmpeg.RxFFmpegCommandList;

public class BilibiliUpload implements CompressorConfig {

    /**
     *  * 上传规则: 视频压缩到720p(720x1280).码率限制到1000kbps.
     * @param inputFilePath
     * @param outFilePath
     * @return
     */
    @Override
    public String[] buildCompressParams(String inputFilePath, String outFilePath, VideoInfo.RealCompressInfo info) {
        RxFFmpegCommandList cmdlist = new RxFFmpegCommandList();
        cmdlist.append("-y");
        cmdlist.append("-i");
        cmdlist.append(inputFilePath);
        //关键在于码率的设置: 4k视频,推荐5000k
        //1080p视频,推荐2084k
        //cmdlist.append("-b");
        // cmdlist.append("1000k");
        //量化比例的范围为0～51，其中0为无损模式，23为缺省值，51可能是最差的
        //若Crf值加6，输出码率大概减少一半；若Crf值减6，输出码率翻倍
        cmdlist.append("-crf");
        cmdlist.append("26");
       /* cmdlist.append("-b");
        cmdlist.append(info.outBitRate/1024+"k");*/
        //cmdlist.append("1500k");
        cmdlist.append("-r");
        cmdlist.append("25");
        String str  =  FFmpegCompressImpl.calScaleStr(1080,inputFilePath);
        if(!TextUtils.isEmpty(str)){
            cmdlist.append("-vf");
            cmdlist.append("scale="+str);//720:1080
        }
        cmdlist.append("-vcodec");
        cmdlist.append("libx264");
        cmdlist.append("-preset");
        cmdlist.append("ultrafast");
        cmdlist.append(outFilePath);
        return cmdlist.build();
    }
}
