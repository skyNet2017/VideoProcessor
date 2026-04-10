package com.hw.videoprocessor.speed.soundtouch;

import androidx.annotation.NonNull;

import com.hw.videoprocessor.speed.AudioTempoProcessor;
import net.surina.soundtouch.SoundTouch;




/**
 * 基于 libsoundtouch 的 {@link AudioTempoProcessor} 实现。
 */
public final class SoundTouchAudioTempoProcessor implements AudioTempoProcessor {

    @Override
    public int applyTempo(@NonNull String wavPath, @NonNull String outPath, float tempo) {
        SoundTouch st = new SoundTouch();
        try {
            st.setTempo(tempo);
            return st.processFile(wavPath, outPath);
        } finally {
            st.close();
        }
    }
}
