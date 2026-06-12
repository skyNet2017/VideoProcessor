package com.hss01248.videocompress.bitrate;

import com.hss01248.videocompress.CompressType;

public interface IBitrateConfig {


    int getExpectedBitRate(@CompressType.Type String compressType);
}
