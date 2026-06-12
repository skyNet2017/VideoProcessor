package com.hss01248.videocompress.listener;

import androidx.annotation.Nullable;

class CompressProgressItem {

    enum Status {
        WAITING,
        RUNNING,
        SUCCESS,
        ERROR,
        CANCELLED
    }

    final String taskKey;
    final String inputPath;
    final long joinOrder;
    final long enqueueTimeMillis;

    @Nullable
    String outputPath;
    Status status = Status.WAITING;
    int progress;
    long startTime;
    @Nullable
    String errorMessage;

    CompressProgressItem(String taskKey, String inputPath, long joinOrder, long enqueueTimeMillis) {
        this.taskKey = taskKey;
        this.inputPath = inputPath;
        this.joinOrder = joinOrder;
        this.enqueueTimeMillis = enqueueTimeMillis;
    }

    boolean canOpenCompare() {
        return status == Status.SUCCESS
                && outputPath != null
                && !outputPath.isEmpty();
    }
}
