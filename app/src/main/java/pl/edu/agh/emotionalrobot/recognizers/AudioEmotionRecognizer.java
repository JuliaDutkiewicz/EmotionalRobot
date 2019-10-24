package pl.edu.agh.emotionalrobot.recognizers;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class AudioEmotionRecognizer implements EmotionRecognizer {

    private static final String LOG_TAG = AudioEmotionRecognizer.class.getSimpleName();
    private static final String INPUT_BUFFER_SIZE = "INPUT_BUFFER_SIZE";
    private static final String OUTPUT_BUFFER_SIZE = "OUTPUT_BUFFER_SIZE";
    private static final String SAMPLE_RATE = "SAMPLE_RATE";
    private static final String RECORDING_LENGTH = "RECORDING_LENGTH";

    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_RECORDING_LENGTH = 44280;
    // minimal buffer length for sample rate 16000 Hz is 1280 => 1296
    // minimal buffer length for sample rate 44100 Hz is 3584 => 3672 - this configuration is recommended
    // because 44100 Hz was the sample rate for training the current neural network

    // neural network size
    private static final int DEFAULT_INPUT_BUFFER_SIZE = 216;
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 10; // warning: must be equals outputNames.size()

    private HashMap<String, Integer> defaultValues = new HashMap<>();
    private int sampleRate;
    private int inputBufferSize;
    private int outputBufferSize; // warning: must be equals outputNames.size()
    private int recordingLength;

    private short[] recordingBuffer;
    private int recordingOffset = 0;
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private short[] audioBuffer;
    private AudioRecord audioRecord;
    private Interpreter interpreter;
    private ArrayList<String> outputNames;

    public AudioEmotionRecognizer(MappedByteBuffer modelFile, String jsonData) {
        this.interpreter = new Interpreter(modelFile);
        initConfigData(jsonData);
        initAudioRecord();
    }

    private void initConfigData(String jsonData) {
        this.outputNames = getOutputNames(jsonData);
        this.defaultValues.put(INPUT_BUFFER_SIZE, DEFAULT_INPUT_BUFFER_SIZE);
        this.defaultValues.put(OUTPUT_BUFFER_SIZE, DEFAULT_OUTPUT_BUFFER_SIZE);
        this.defaultValues.put(SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
        this.defaultValues.put(RECORDING_LENGTH, DEFAULT_RECORDING_LENGTH);
        this.inputBufferSize = getDataValue(jsonData, INPUT_BUFFER_SIZE);
        this.outputBufferSize = getDataValue(jsonData, OUTPUT_BUFFER_SIZE);
        this.sampleRate = getDataValue(jsonData, SAMPLE_RATE);
        this.recordingLength = getDataValue(jsonData, RECORDING_LENGTH);
        this.recordingBuffer = new short[recordingLength];
    }

    private int getDataValue(String jsonData, String key) {
        try {
            JSONObject obj = new JSONObject(jsonData);
            return obj.getInt(key);
        } catch (JSONException e) {
            Log.v(LOG_TAG, "Error while reading json, " + key + " set to default value ");
        }
        return defaultValues.get(key);
    }

    private ArrayList<String> getOutputNames(String jsonData) {
        ArrayList<String> outputNames = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(jsonData);
            JSONArray names = obj.getJSONArray("names");

            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                outputNames.add(name);
            }
        } catch (JSONException e) {
            Log.v(LOG_TAG, "Error while reading json");
        }
        return outputNames;
    }

    private void initAudioRecord() {
        int bufferSize = getBufferSize();
        this.audioBuffer = new short[bufferSize / 2];
        this.audioRecord =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        sampleRate,
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
                        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = sampleRate * 2;
        }
        return bufferSize;
    }

    private Map<String, Float> recognize() {
        short[] inputBuffer = new short[recordingLength];

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

        float[][] outputFull = new float[1][outputBufferSize];
        for (int j = 0; j < outputBufferSize; j++) {
            outputFull[0][j] += 0;
        }

        // cannot be greater because of neural network input size
        int REPEATS_TIMES = (int) recordingLength / inputBufferSize;
        for (int k = 0; k < REPEATS_TIMES; k++) {
            float[] floatInputBuffer = preProcessing(inputBuffer, k);
            float[][] output = new float[1][outputBufferSize];
            interpreter.run(floatInputBuffer, output);
            for (int j = 0; j < outputBufferSize; j++) {
                outputFull[0][j] += output[0][j];
            }
        }
        for (int j = 0; j < outputBufferSize; j++) {
            outputFull[0][j] /= REPEATS_TIMES;
        }
        audioRecord.stop();
        return postProcessing(outputFull[0]);
    }

    // process recorded audio to buffer required by neural network
    private float[] preProcessing(short[] inputBuffer, int k) {
        float[] floatInputBuffer = new float[inputBufferSize];
        for (int i = 0; i < inputBufferSize; i++) {
            floatInputBuffer[i] = inputBuffer[(i + (k * inputBufferSize)) % recordingLength];
        }
        return floatInputBuffer;
    }

    private HashMap<String, Float> postProcessing(float[] floats) {
        HashMap<String, Float> results = new HashMap<>();
        StringBuilder resultsText = new StringBuilder("Results: \n");
        for (int i = 0; i < outputBufferSize; i++) {
            results.put(this.outputNames.get(i), floats[i]);
            resultsText.append(outputNames.get(i)).append(": ").append(floats[i]).append("\n");
        }
        Log.v(LOG_TAG, resultsText.toString());
        return results;
    }
}
