package com.hss01248.videocompress.bitrate;



/**
 * @Despciption todo
 * @Author hss
 * @Date 9/27/24 2:36 PM
 * @Version 1.0
 */
public class LowThanBiliBitrateConfig extends BilibiliBitrateConfig{

    @Override
    public int getExpectedBitRate(String compressType) {
        return Math.round(super.getExpectedBitRate(compressType)*0.8f);
    }
}
