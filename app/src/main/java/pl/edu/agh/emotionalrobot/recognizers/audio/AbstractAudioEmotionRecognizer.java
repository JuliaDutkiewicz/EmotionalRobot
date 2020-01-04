package pl.edu.agh.emotionalrobot.recognizers.audio;

import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public abstract class AbstractAudioEmotionRecognizer implements EmotionRecognizer {
    private static final String INPUT_BUFFER_SIZE = "INPUT_BUFFER_SIZE";
    private static final String OUTPUT_BUFFER_SIZE = "OUTPUT_BUFFER_SIZE";
    private static final String SAMPLE_RATE = "SAMPLE_RATE";
    private static final String RECORDING_LENGTH = "RECORDING_LENGTH";
    private static final String NN_NAME = "NN_NAME";

    private static final String LOG_TAG = AbstractAudioEmotionRecognizer.class.getSimpleName();
    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_RECORDING_LENGTH = 44100;
    // minimal buffer length for sample rate 16000 Hz is 1280
    // minimal buffer length for sample rate 44100 Hz is 3584

    // neural network size
    private static final int DEFAULT_INPUT_BUFFER_SIZE = 216;
    private String nnName;
    private int inputBufferSize;
    private int outputBufferSize; // warning: must be equals outputNames.size()
    private int recordingLength;
    private Interpreter interpreter;
    private ArrayList<String> outputNames;
    private int sampleRate;
    private Microphone microphone;
    private HashMap<String, Integer> defaultValues = new HashMap<>();

    public AbstractAudioEmotionRecognizer(MappedByteBuffer modelFile, String jsonData) {
        this.interpreter = new Interpreter(modelFile);
        initConfigData(jsonData);
    }

    private void initConfigData(String jsonData) {
        this.outputNames = getOutputNames(jsonData);
        this.defaultValues.put(INPUT_BUFFER_SIZE, DEFAULT_INPUT_BUFFER_SIZE);
        this.defaultValues.put(OUTPUT_BUFFER_SIZE, this.outputNames.size());
        this.defaultValues.put(SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
        this.defaultValues.put(RECORDING_LENGTH, DEFAULT_RECORDING_LENGTH);
        this.nnName = getDataString(jsonData, NN_NAME);
        this.inputBufferSize = getDataValue(jsonData, INPUT_BUFFER_SIZE);
        this.outputBufferSize = outputNames.size();
        this.sampleRate = getDataValue(jsonData, SAMPLE_RATE);
        this.microphone = new Microphone(sampleRate);
        this.recordingLength = getDataValue(jsonData, RECORDING_LENGTH);
        this.microphone.setRecordingBuffer(new short[recordingLength]);
        this.microphone.initAudioRecord();
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



    private String getDataString(String jsonData, String key) {
        try {
            JSONObject obj = new JSONObject(jsonData);
            return obj.getString(key);
        } catch (JSONException e) {
            Log.v(LOG_TAG, "Error while reading json, " + key + " set to default value ");
        }
        return "";
    }

    private ArrayList<String> getOutputNames(String jsonData) {
        ArrayList<String> outputNames = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(jsonData);
            JSONArray names = obj.getJSONArray("EMOTIONS");

            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                outputNames.add(name);
            }
        } catch (JSONException e) {
            Log.v(LOG_TAG, "Error while reading json");
        }
        return outputNames;
    }

    abstract float[] preProcess(short[] inputBuffer);

    abstract HashMap<String, Float> postProcess(float[] floats);

    @Override
    public Pair<Map<String, Float>, byte[]> getEmotionsWithRawData() {
        short[] audioBuffer = microphone.getRecordedAudioBuffer(recordingLength);
        Map<String, Float> emotions = recognize(audioBuffer);
        byte[] rawData = extractRawData(audioBuffer);
        return new Pair<>(emotions, rawData);
    }

    @Override
    public Map<String, Float> getEmotions() {
        short[] inputBuffer = microphone.getRecordedAudioBuffer(recordingLength);
        return recognize(inputBuffer);
    }

    @Override
    public byte[] getRawData() {
        short[] audioBuffer = microphone.getRecordedAudioBuffer(recordingLength);
        return extractRawData(audioBuffer);
    }


    private Map<String, Float> recognize(short[] inputBuffer) {
//      LibrosaMFCC
        float[][] outputFull = new float[1][outputBufferSize];
        for (int j = 0; j < outputBufferSize; j++) {
            outputFull[0][j] += 0;
        }
        float[] mfccInput = preProcess(inputBuffer);
        int REPEATS_TIMES = (int) mfccInput.length / inputBufferSize;
        for (int k = 0; k < REPEATS_TIMES; k++) {
            float[] floatInputBuffer = getRequiredSizeFrame(mfccInput, k);
            float[][] output = new float[1][outputBufferSize];
            interpreter.run(floatInputBuffer, output);
            for (int j = 0; j < outputBufferSize; j++) {
                outputFull[0][j] += output[0][j];
            }
        }
        for (int j = 0; j < outputBufferSize; j++) {
            outputFull[0][j] /= REPEATS_TIMES;
        }
        return postProcess(outputFull[0]);
    }

    // process recorded audio to buffer required by neural network
    private float[] getRequiredSizeFrame(float[] mfccInput, int k) {
        float[] floatInputBuffer = new float[inputBufferSize];
        for (int i = 0; i < inputBufferSize; i++) {
            floatInputBuffer[i] = (float) mfccInput[(i * k + (inputBufferSize)) % mfccInput.length];
        }
        return floatInputBuffer;
    }

    private byte[] extractRawData(short[] audioBuffer) {
        float[] floatAudioBuffer = shortToFloat(audioBuffer);
        return floatArray2ByteArray(floatAudioBuffer);
    }

    private static float[] shortToFloat(short[] shortArray) {
        float[] floatOut = new float[shortArray.length];
        for (int i = 0; i < shortArray.length; i++) {
            floatOut[i] = shortArray[i] / 32768.0f;
        }
        return floatOut;
    }

    private static byte[] floatArray2ByteArray(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);
        for (float value : values) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    @Override
    public String getType() {
        return "audio";
    }

    String getNnName() {
        return nnName;
    }

    ArrayList<String> getOutputNames() {
        return outputNames;
    }

    int getOutputBufferSize() {
        return outputBufferSize;
    }

    @Override
    public void destroy(){

    }
}
