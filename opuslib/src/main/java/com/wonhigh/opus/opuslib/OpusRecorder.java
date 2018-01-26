package com.wonhigh.opus.opuslib;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.wonhigh.opus.opuslib.model.AudioTime;
import com.wonhigh.opus.opuslib.utils.LogHelper;
import com.wonhigh.opus.opuslib.utils.Utils;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.AudioRecord.ERROR_INVALID_OPERATION;
import static com.wonhigh.opus.opuslib.OpusEvent.RECORD_FAILED;
import static com.wonhigh.opus.opuslib.OpusEvent.RECORD_FINISHED;

/**
 * 录音处理类
 */
public class OpusRecorder {
    private static final String TAG = LogHelper.makeLogTag(OpusRecorder.class);

    //录音文件保存名称前缀
    public static String fileHeaderName = "OpusRecord";
    public static String pcmHeaderName = "PcmRecord";

    //录音机状态
    private static final int STATE_NONE = 0;
    private static final int STATE_STARTED = 1;
    private volatile int state = STATE_NONE;

    //音频采样率
    private static final int RECORDER_SAMPLERATE = 16000;
    //设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    //音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    //音频大小
    private int bufferSize = 0;

    private AudioRecord recorder = null;

    private Thread recordingThread = new Thread();
    private Thread pcmThread = new Thread();

    private OpusTool opusTool = new OpusTool();
    private static volatile OpusRecorder opusRecorder;


    // Should be 1920, to meet with function writeFrame()
    private ByteBuffer fileBuffer = ByteBuffer.allocateDirect(1920);

    private String filePath = null;
    private String pcmPath = null;
    private OpusEvent eventSender = null;
    private Timer progressTimer = null;
    private AudioTime recordTime = new AudioTime();

    private OpusRecorder() {
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static OpusRecorder getInstance() {
        if (opusRecorder == null) {
            synchronized (OpusRecorder.class) {
                if (opusRecorder == null) {
                    opusRecorder = new OpusRecorder();
                }
            }
        }
        return opusRecorder;
    }

    public void setEventSender(OpusEvent es) {
        eventSender = es;
    }

    /**
     * 开始录音
     *
     * @param file
     */
    public void startRecording(final String file) {
        if (state == STATE_STARTED) return;

        int minBufferSize = AudioRecord.getMinBufferSize(
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING
        );

        bufferSize = (minBufferSize / 1920 + 1) * 1920;

        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                bufferSize
        );

        recorder.startRecording();

        state = STATE_STARTED;
        if (file.isEmpty()) {
            filePath = OpusTrackInfo.getInstance().getAValidFileName(fileHeaderName);
            pcmPath = OpusTrackInfo.getInstance().getPcmFileName(pcmHeaderName);
        } else {
            filePath = file;
            pcmPath = file;
        }
//        filePath = file.isEmpty() ? initRecordFileName() : file;
        int rst = opusTool.startRecording(filePath);
        if (rst != 1) {
            if (eventSender != null) {
                eventSender.sendEvent(RECORD_FAILED);
            }
            LogHelper.e(TAG, "recorder initially error");
            return;
        }

        if (eventSender != null) {
            eventSender.sendEvent(OpusEvent.RECORD_STARTED);
        }

        recordingThread = new Thread(new RecordThread(), "OpusRecord Thread");
        recordingThread.start();

        pcmThread = new Thread(new RecordPCMThread(), "PcmRecord Thread");
        pcmThread.start();


    }

    /**
     * 保存PCM格式文件
     */
    private void writeAudioDataToPCM() {
        try {
            //输出流
            OutputStream os = new FileOutputStream(pcmPath);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);


            short[] buffer = new short[bufferSize];

            while (state == STATE_STARTED) {
                int len = recorder.read(buffer, 0, bufferSize);
                if (len != ERROR_INVALID_OPERATION) {
                    for (int i = 0; i < len; i++) {
                        dos.writeShort(buffer[i]);
                    }
                }
            }

            dos.close();
        } catch (Throwable t) {
            Log.e(TAG, "录音失败");
        }
    }

    /**
     * 将数据写入文件
     *
     * @param buffer
     * @param size
     */
    private void writeAudioDataToOpus(ByteBuffer buffer, int size) {
        ByteBuffer finalBuffer = ByteBuffer.allocateDirect(size);
        finalBuffer.put(buffer);
        finalBuffer.rewind();
        boolean flush = false;

        // write data to Opus file
        while (state == STATE_STARTED && finalBuffer.hasRemaining()) {
            int oldLimit = -1;
            if (finalBuffer.remaining() > fileBuffer.remaining()) {
                oldLimit = finalBuffer.limit();
                finalBuffer.limit(fileBuffer.remaining() + finalBuffer.position());
            }
            fileBuffer.put(finalBuffer);
            if (fileBuffer.position() == fileBuffer.limit() || flush) {
                int length = !flush ? fileBuffer.limit() : finalBuffer.position();

                int rst = opusTool.writeFrame(fileBuffer, length);
                if (rst != 0) {
                    fileBuffer.rewind();
                }
            }
            if (oldLimit != -1) {
                finalBuffer.limit(oldLimit);
            }
        }
    }

    /**
     * 将录音输输出流数据写入文件
     */
    private void writeAudioDataToFile() {
        if (state != STATE_STARTED)
            return;


        //opus编码
        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        while (state == STATE_STARTED) {
            buffer.rewind();
            int len = recorder.read(buffer, bufferSize);
            LogHelper.d(TAG, "\n bufferSize's length is " + len);
            if (len != ERROR_INVALID_OPERATION) {
                try {
                    writeAudioDataToOpus(buffer, len);
                } catch (Exception e) {
                    if (eventSender != null)
                        eventSender.sendEvent(RECORD_FAILED);
                    Utils.printE(TAG, e);
                }
            }
        }
    }

    private void updateTrackInfo() {
        OpusTrackInfo info = OpusTrackInfo.getInstance();
        info.addOpusFile(filePath);
        if (eventSender != null) {
            File f = new File(filePath);
            eventSender.sendEvent(RECORD_FINISHED, f.getName());
        }
    }

    /**
     * 停止录音
     */
    public void stopRecording() {
        if (state != STATE_STARTED) return;

        state = STATE_NONE;
        progressTimer.cancel();
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            Utils.printE(TAG, e);
        }

        if (null != recorder) {
            opusTool.stopRecording();
            recordingThread = null;
            recorder.stop();
            recorder.release();
            recorder = null;
        }

        updateTrackInfo();
    }

    public boolean isWorking() {
        return state != STATE_NONE;
    }

    public void release() {
        if (state != STATE_NONE) {
            stopRecording();
        }
    }

    private class ProgressTask extends TimerTask {
        public void run() {
            if (state != STATE_STARTED) {
                progressTimer.cancel();
            } else {
                recordTime.add(1);
                String progress = recordTime.getTime();
                if (eventSender != null) {
                    eventSender.sendRecordProgressEvent(progress);
                }
            }
        }
    }


    private class RecordThread implements Runnable {
        public void run() {
            progressTimer = new Timer();
            recordTime.setTimeInSecond(0);
            progressTimer.schedule(new ProgressTask(), 1000, 1000);

            writeAudioDataToFile();
        }
    }


    private class RecordPCMThread implements Runnable {

        @Override
        public void run() {
            writeAudioDataToPCM();
        }
    }
}
