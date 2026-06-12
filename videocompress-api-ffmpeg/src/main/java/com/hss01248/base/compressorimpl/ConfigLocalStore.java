package com.hss01248.base.compressorimpl;

import com.hss01248.videocompress.CompressorConfig;
import com.hss01248.videocompress.VideoInfo;

import io.microshow.rxffmpeg.RxFFmpegCommandList;

public class ConfigLocalStore implements CompressorConfig {
    @Override
    public String[] buildCompressParams(String inputFilePath, String outFilePath, VideoInfo.RealCompressInfo info) {
        RxFFmpegCommandList cmdlist = new RxFFmpegCommandList();
        cmdlist.append("-y");
        cmdlist.append("-i");
        cmdlist.append(inputFilePath);
        //关键在于码率的设置: 4k视频,推荐5000k
        //1080p视频,推荐2084k
        //cmdlist.append("-b");
        //cmdlist.append("6000k");
        //量化比例的范围为0～51，其中0为无损模式，23为缺省值，51可能是最差的
        //若Crf值加6，输出码率大概减少一半；若Crf值减6，输出码率翻倍
        cmdlist.append("-crf");
        cmdlist.append("26");
        //cmdlist.append("1500k");
        cmdlist.append("-r");
        cmdlist.append("30");
        //cmdlist.append("-vf");
        //cmdlist.append("scale=720:1080");
        cmdlist.append("-vcodec");
        cmdlist.append("libx264");
        cmdlist.append("-preset");
        cmdlist.append("superfast");
        cmdlist.append(outFilePath);
        return cmdlist.build();
    }
}
