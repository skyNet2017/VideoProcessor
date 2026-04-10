package com.hw.videoprocessor.speed;

import org.jetbrains.annotations.NotNull;

/**
 * 音频变速（tempo）处理，由业务通过 {@link com.hw.videoprocessor.VideoProcessor#setAudioTempoProcessor}
 * 注入实现。未设置时非 1 倍速按 1 倍速处理；实现返回失败时同样回退为 1 倍速。
 */
public interface AudioTempoProcessor {

    /**
     * @return 与 SoundTouch.processFile 一致：&gt;= 0 成功，&lt; 0 失败
     */
    int applyTempo(@NotNull String wavPath, @NotNull String outPath, float tempo);
}
