package pl.edu.agh.emotionalrobot.recognizers.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import javax.annotation.PreDestroy;

import java.util.concurrent.locks.ReentrantLock;

public class Microphone {

    private static final String LOG_TAG = Microphone.class.getSimpleName();
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private int recordingOffset = 0;
    private short[] recordingBuffer;
    private short[] audioBuffer;
    private AudioRecord audioRecord;
    private int sampleRate;

    public Microphone(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public void initAudioRecord() {
        int bufferSize = getBufferSize();
        this.audioBuffer = new short[bufferSize / 2];
        this.audioRecord =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            initAudioRecord();
        }

        audioRecord.startRecording();
    }

    public void record() {
        int numberRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
        int maxLength = recordingBuffer.length;
        int newRecordingOffset = recordingOffset + numberRead;
        int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
        int firstCopyLength = numberRead - secondCopyLength;

        recordingBufferLock.lock();
        try {
            System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
            System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
            recordingOffset = newRecordingOffset % maxLength;
        } catch (Exception e) {
            Log.v(LOG_TAG, "Buffer warning.");
        } finally {
            recordingBufferLock.unlock();
        }
    }

    private int getBufferSize() {
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = sampleRate * 2;
        }
        return bufferSize;
    }

    public ReentrantLock getRecordingBufferLock() {
        return recordingBufferLock;
    }

    public int getRecordingOffset() {
        return recordingOffset;
    }

    public short[] getRecordingBuffer() {
        return recordingBuffer;
    }

    public void setRecordingBuffer(short[] recordingBuffer) {
        this.recordingBuffer = recordingBuffer;
    }

    short[] getRecordedAudioBuffer(int recordingLength) {
        record();
        short[] inputBuffer = new short[recordingLength];

        getRecordingBufferLock().lock();
        try {
            int maxLength = getRecordingBuffer().length;
            int firstCopyLength = maxLength - getRecordingOffset();
            int secondCopyLength = getRecordingOffset();
            System.arraycopy(getRecordingBuffer(), getRecordingOffset(), inputBuffer, 0, firstCopyLength);
            System.arraycopy(getRecordingBuffer(), 0, inputBuffer, firstCopyLength, secondCopyLength);
        } catch (Exception e) {
            Log.v(LOG_TAG, "Buffer warning.");
        } finally {
            getRecordingBufferLock().unlock();
        }
        return inputBuffer;
    }

    @PreDestroy
    public void stopRecording() {
        audioRecord.stop();
    }
}
