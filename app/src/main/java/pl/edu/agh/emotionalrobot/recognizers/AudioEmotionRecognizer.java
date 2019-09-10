package pl.edu.agh.emotionalrobot.recognizers;

import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.FEMALE_ANGRY;
import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.FEMALE_CALM;
import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.FEMALE_FEARFUL;
import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.FEMALE_HAPPY;
import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.FEMALE_SAD;
import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.MALE_ANGRY;
import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.MALE_CALM;
import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.MALE_FEARFUL;
import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.MALE_HAPPY;
import static pl.edu.agh.emotionalrobot.recognizers.AudioUtils.MALE_SAD;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class AudioEmotionRecognizer implements EmotionRecognizer {

    private static final int SAMPLE_RATE = 16000;
    private static final int RECORDING_LENGTH = 1296;
    // lower cannot work -> minimal buffer length for sample rate 16000 is 1280
    private static final int INPUT_BUFFER_SIZE = 216;
    // cannot be greater because of neural network input size

    private static final String LOG_TAG = AudioEmotionRecognizer.class.getSimpleName();

    private short[] recordingBuffer = new short[RECORDING_LENGTH];
    private int recordingOffset = 0;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private short[] audioBuffer;
    private AudioRecord audioRecord;
    private Interpreter interpreter;

    public AudioEmotionRecognizer(MappedByteBuffer modelFile) {
        this.interpreter = new Interpreter(modelFile);
        initAudioRecord();
    }

    private void initAudioRecord() {
        int bufferSize = getBufferSize();
        this.audioBuffer = new short[bufferSize / 2];
        this.audioRecord =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);
    }

    @Override
    public Map<String, Float> getEmotions() {
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            initAudioRecord();
            return null;
        }
        record();
        return recognize();
    }

    private void record() {
        audioRecord.startRecording();
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
        } finally {
            recordingBufferLock.unlock();
        }
    }

    private int getBufferSize() {
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        return bufferSize;
    }

    private Map<String, Float> recognize() {
        short[] inputBuffer = new short[RECORDING_LENGTH];
        float[] floatInputBuffer = new float[INPUT_BUFFER_SIZE];

        recordingBufferLock.lock();
        try {
            int maxLength = recordingBuffer.length;
            int firstCopyLength = maxLength - recordingOffset;
            int secondCopyLength = recordingOffset;
            System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
            System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
        } finally {
            recordingBufferLock.unlock();
        }

        // We need to feed in float values between -1.0f and 1.0f, so divide the
        // signed 16-bit inputs.
        for (int i = 0; i < INPUT_BUFFER_SIZE; i++) {
            floatInputBuffer[i] = inputBuffer[i] / 32767.0f;
        }

        // Run the model.
        float[][] output = new float[1][10];
        interpreter.run(floatInputBuffer, output);
        HashMap<String, Float> results = new HashMap<>();
        results.put(FEMALE_ANGRY, output[0][0]);
        results.put(FEMALE_CALM, output[0][1]);
        results.put(FEMALE_FEARFUL, output[0][2]);
        results.put(FEMALE_HAPPY, output[0][3]);
        results.put(FEMALE_SAD, output[0][4]);
        results.put(MALE_ANGRY, output[0][5]);
        results.put(MALE_CALM, output[0][6]);
        results.put(MALE_FEARFUL, output[0][7]);
        results.put(MALE_HAPPY, output[0][8]);
        results.put(MALE_SAD, output[0][9]);

        Log.v(LOG_TAG, FEMALE_ANGRY + " " + output[0][0]
                + "\n" + FEMALE_CALM + " " + output[0][1]
                + "\n" + FEMALE_FEARFUL + " " + output[0][2]
                + "\n" + FEMALE_HAPPY + " " + output[0][3]
                + "\n" + FEMALE_SAD + " " + output[0][4]
                + "\n" + MALE_ANGRY + " " + output[0][5]
                + "\n" + MALE_CALM + " " + output[0][6]
                + "\n" + MALE_FEARFUL + " " + output[0][7]
                + "\n" + MALE_HAPPY + " " + output[0][8]
                + "\n" + MALE_SAD + " " + output[0][9]
        );
        audioRecord.stop();
        return results;
    }
}
