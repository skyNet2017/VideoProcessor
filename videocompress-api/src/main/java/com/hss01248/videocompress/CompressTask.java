package com.hss01248.videocompress;

import androidx.annotation.Nullable;

import com.hss01248.videocompress.listener.ICompressListener;

class CompressTask {

    final boolean async;
    final String inputPath;
    @Nullable
    final String outDir;
    @CompressType.Type
    final String compressType;
    final ICompressListener listener;
    @Nullable
    final String progressTaskKey;

    CompressTask(boolean async, String inputPath, @Nullable String outDir,
                 @CompressType.Type String compressType, ICompressListener listener) {
        this(async, inputPath, outDir, compressType, listener, null);
    }

    CompressTask(boolean async, String inputPath, @Nullable String outDir,
                 @CompressType.Type String compressType, ICompressListener listener,
                 @Nullable String progressTaskKey) {
        this.async = async;
        this.inputPath = inputPath;
        this.outDir = outDir;
        this.compressType = compressType;
        this.listener = listener;
        this.progressTaskKey = progressTaskKey;
    }
}
