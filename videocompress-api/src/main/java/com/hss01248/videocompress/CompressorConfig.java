package com.hss01248.videocompress;

public interface CompressorConfig {

    /**
     * r20-b3000 137M->15.1M 50s  3210bps
     * r20  137M->112.7M 67s  23483 kbs
     * <p>
     * r20-b4096 137M->20M 55s
     * <p>
     * r23-b4096 38M->6 14s
     *
     *  * ffmpeg -y -i /storage/emulated/0/1/input.mp4 -b 2097k -r 30 -vcodec libx264 -preset superfast /storage/emulated/0/1/result.mp4
     * <p>
     * 码率推荐; https://blog.csdn.net/benkaoya/article/details/79558896
     * <p>
     * https://blog.csdn.net/rootusers/article/details/41646557
     * <p>
     * https://blog.csdn.net/DONGHONGBAI/article/details/84776431
     * <p>
     * https://blog.csdn.net/Martin_chen2/article/details/105772872
     * <p>
     * crf vs b
     */
    String[] buildCompressParams(String inputFilePath, String outFilePath,VideoInfo.RealCompressInfo info);

    default String compressedFileName(){
        return getClass().getName()+".mp4";
    }
}
