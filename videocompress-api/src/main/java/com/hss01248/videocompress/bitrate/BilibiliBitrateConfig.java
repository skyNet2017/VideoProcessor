package com.hss01248.videocompress.bitrate;

import com.hss01248.videocompress.CompressType;

/**
 * @Despciption todo
 * @Author hss
 * @Date 9/27/24 2:11 PM
 * @Version 1.0
 */
public class BilibiliBitrateConfig implements IBitrateConfig{
    @Override
    public int getExpectedBitRate(String compressType) {
        int expect = 1500;
        if(CompressType.TYPE_SDR_1080P.equals(compressType)){
            //8000kbps
            expect = 6*1024*1024;
        }else if(CompressType.TYPE_SDR_720P.equals(compressType)){
            expect = 3*1024*1024;
        }else  if(compressType.equals(CompressType.TYPE_SDR_360P)){
            expect = 1024*1024;
        }else  if(compressType.equals(CompressType.TYPE_SDR_480P)){
            expect = (int) (2*1024*1024);
        }else  if(compressType.equals(CompressType.TYPE_HDR_720P)){
            expect = (int) (3*1024*1024);
        }else  if(compressType.equals(CompressType.TYPE_HDR_1080P)){
            expect = 6*1024*1024;
        }else  if(compressType.equals(CompressType.TYPE_HDR_2K)){
            expect = 10*1024*1024;
        }else  if(compressType.equals(CompressType.TYPE_HDR_4K)){
            expect = 20*1024*1024;
        }
        return expect;
    }
}
