package com.wonhigh.opus.opuslib;


import com.wonhigh.opus.opuslib.utils.LogHelper;

import java.nio.ByteBuffer;

/**
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Dec-26
 */
public class OpusTool {
    private static final String TAG = LogHelper.makeLogTag(OpusTool.class);

    static {
        try {
            System.loadLibrary("Belleopus");
            LogHelper.d(TAG, "loaded library ");
        } catch (UnsatisfiedLinkError e) {
            LogHelper.e(TAG, "Could not load library ");
        }
    }

    /**
     * Get library information
     *
     * @return version information
     */
    public native String nativeGetString();

    /**
     * 编码
     *
     * @param wavFile  path to input Wav file
     * @param opusFile path to output opus file
     * @param option   option for encoding
     * @return non zero if successful
     */
    public native int encode(String wavFile, String opusFile, String option);

    /**
     * 解码
     *
     * @param opusFile path to input opus file
     * @param wavFile  path to output Wav file
     * @param option   option for encoding
     * @return non zero if successful
     */
    public native int decode(String opusFile, String wavFile, String option);

    /**
     * Start opus recording
     * 开始录音
     *
     * @param opusFile path to save opus file
     * @return non zero if started player
     */
    public native int startRecording(String opusFile);


    /**
     * Stop recording
     * 停止录音
     */
    public native void stopRecording();

    /**
     * Play opus file
     * 播放录音
     *
     * @param opusFile path of opus file which is to be played
     * @return
     */
    public native int play(String opusFile);

    /**
     * Stop playing opusfile
     * 停止播放
     */
    public native void stopPlaying();

    /**
     * Writing audio frame to encoder
     *
     * @param frame buffer with sound in 16 bit mono PCM 16000 format
     * @param len   len of data
     * @return not null if successful
     */
    public native int writeFrame(ByteBuffer frame, int len);

    /**
     * Checking Opus File format
     *
     * @param path path to file
     * @return non zero if opus file
     */
    public native int isOpusFile(String path);

    /**
     * Opening file
     *
     * @param path path to file
     * @return non zero if successful
     */
    public native int openOpusFile(String path);

    /**
     * Seeking in opus file
     *
     * @param position position in file
     * @return non zero if successful
     */
    public native int seekOpusFile(float position);

    /**
     * Closing opus file
     */
    public native void closeOpusFile();

    /**
     * Reading from opus file
     *
     * @param buffer
     * @param capacity
     */
    public native void readOpusFile(ByteBuffer buffer, int capacity);

    /**
     * Is playback finished
     *
     * @return non zero if playback is finished
     */
    public native int getFinished();

    /**
     * Read block size in readOpusFile
     *
     * @return block size in bytes
     */
    public native int getSize();

    /**
     * return the Chanel account of current opus file
     *
     * @return Channel account
     */
    public native int getChannelCount();

    /**
     * Offset of actual sound for playback
     *
     * @return offset
     */
    public native long getPcmOffset();

    /**
     * Total opus pcm duration
     *
     * @return pcm duration
     */
    public native long getTotalPcmDuration();

    /**
     * Offset of actual sound for playback, whose units is seconds
     *
     * @return time
     */
    public long getCurrentPosition() {
        return getPcmOffset() / 48000;
    }

    /**
     * Total duration of an opus file, whose units is second
     *
     * @return pcm duration
     */
    public long getTotalDuration() {
        return getTotalPcmDuration() / 48000;
    }

}
