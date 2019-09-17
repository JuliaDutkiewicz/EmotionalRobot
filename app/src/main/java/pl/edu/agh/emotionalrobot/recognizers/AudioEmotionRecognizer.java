package pl.edu.agh.emotionalrobot.recognizers;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class AudioEmotionRecognizer implements EmotionRecognizer {

    //
    private static final int SAMPLE_RATE = 44100;
    private static final int RECORDING_LENGTH = 44280;
    // minimal buffer length for sample rate 16000 Hz is 1280 => 1296
    // minimal buffer length for sample rate 44100 Hz is 3584 => 3672 - this configuration is recommended
    // because 44100 Hz was the sample rate for training the current neural network

    // neural network size
    private static final int INPUT_BUFFER_SIZE = 216;
    private static final int OUTPUT_BUFFER_SIZE = 10; // warning: must be equals outputNames.size()

    // cannot be greater because of neural network input size
    private static final int REPEATS_TIMES = (int) RECORDING_LENGTH / INPUT_BUFFER_SIZE;

    private static final String LOG_TAG = AudioEmotionRecognizer.class.getSimpleName();

    private short[] recordingBuffer = new short[RECORDING_LENGTH];
    private int recordingOffset = 0;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private short[] audioBuffer;
    private AudioRecord audioRecord;
    private Interpreter interpreter;
    private final ArrayList<String> outputNames;

    public AudioEmotionRecognizer(MappedByteBuffer modelFile, ArrayList<String> outputNames) {
        this.interpreter = new Interpreter(modelFile);
        this.outputNames = outputNames;
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

        float[][] outputFull = new float[1][OUTPUT_BUFFER_SIZE];
        for (int j = 0; j < OUTPUT_BUFFER_SIZE; j++){
            outputFull[0][j] += 0;
        }

        for (int k = 0; k < REPEATS_TIMES; k++) {
            float[] floatInputBuffer = preProcessing(inputBuffer, k);
            float[][] output = new float[1][OUTPUT_BUFFER_SIZE];
            interpreter.run(floatInputBuffer, output);
            for (int j = 0; j < OUTPUT_BUFFER_SIZE; j++){
                outputFull[0][j] += output[0][j];
            }
        }
        for (int j = 0; j < OUTPUT_BUFFER_SIZE; j++){
            outputFull[0][j] /= REPEATS_TIMES;
        }
        audioRecord.stop();
        return postProcessing(outputFull[0]);
    }

    // process recorded audio to buffer required by neural network
    private float[] preProcessing(short[] inputBuffer, int k) {
        float[] floatInputBuffer = new float[INPUT_BUFFER_SIZE];
        for (int i = 0; i < INPUT_BUFFER_SIZE; i++) {
            floatInputBuffer[i] = inputBuffer[(i + (k * INPUT_BUFFER_SIZE)) % RECORDING_LENGTH];
        }
        return floatInputBuffer;
    }

    private HashMap<String, Float> postProcessing(float[] floats) {
        HashMap<String, Float> results = new HashMap<>();
        StringBuilder resultsText = new StringBuilder("Results: \n");
        for (int i = 0; i < OUTPUT_BUFFER_SIZE; i++) {
            results.put(this.outputNames.get(i), floats[i]);
            resultsText.append(outputNames.get(i)).append(": ").append(floats[i]).append("\n");
        }
        Log.v(LOG_TAG, resultsText.toString());
        return results;
    }
}
