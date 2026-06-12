package com.hss01248.videocompress;

import androidx.annotation.StringDef;

public interface CompressType {

    String TYPE_SDR_480P = "sdr-480p";
    String TYPE_SDR_360P = "sdr-360p";
    String TYPE_SDR_720P = "sdr-720p";
    String TYPE_SDR_1080P = "sdr-1080p";

    String TYPE_HDR_720P = "hdr-720p";
    String TYPE_HDR_1080P = "hdr-1080p";

    String TYPE_HDR_2K = "hdr-2k";
    String TYPE_HDR_4K = "hdr-4k";

    String TYPE_FOR_STORE = "for-store";



    @StringDef({TYPE_SDR_720P, TYPE_SDR_1080P,TYPE_SDR_480P,
            TYPE_SDR_360P,TYPE_HDR_720P,TYPE_HDR_1080P,TYPE_HDR_2K,TYPE_HDR_4K,TYPE_FOR_STORE})
    public @interface Type {

    }

    static int typeToResolution(String compressType){
        int targetResolution = 720;
        if(CompressType.TYPE_SDR_720P.equals(compressType)){
            targetResolution = 720;
        }else if(CompressType.TYPE_SDR_1080P.equals(compressType)){
            targetResolution = 1080;
        }else if(CompressType.TYPE_SDR_360P.equals(compressType)){
            targetResolution = 360;
        }else if(CompressType.TYPE_SDR_480P.equals(compressType)){
            targetResolution = 480;
        }else if(CompressType.TYPE_HDR_720P.equals(compressType)){
            targetResolution = 720;
        }else if(CompressType.TYPE_HDR_1080P.equals(compressType)){
            targetResolution = 1080;
        }else if(CompressType.TYPE_HDR_2K.equals(compressType)){
            targetResolution = 1440;
        }else if(CompressType.TYPE_HDR_4K.equals(compressType)){
            targetResolution = 2160;
        }else if(CompressType.TYPE_FOR_STORE.equals(compressType)){
            targetResolution = 2160;
        }
        return targetResolution;
    }

    static String resolutionToType(int resolution ){
        if(resolution < 480){
            return CompressType.TYPE_SDR_360P;
        }else if(resolution < 720){
            return CompressType.TYPE_SDR_480P;
        }else if(resolution < 1080){
            return CompressType.TYPE_SDR_720P;
        }else if(resolution < 1440){
            return CompressType.TYPE_SDR_1080P;
        }else if(resolution < 2560){
            return CompressType.TYPE_HDR_2K;
        }else {
            return CompressType.TYPE_HDR_4K;
        }
    }
}
