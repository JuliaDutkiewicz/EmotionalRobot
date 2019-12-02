package pl.edu.agh.emotionalrobot.recognizers.audio;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.PreDestroy;

import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public abstract class AbstractAudioEmotionRecognizer implements EmotionRecognizer {
    private static final String LOG_TAG = AbstractAudioEmotionRecognizer.class.getSimpleName();

    static final String INPUT_BUFFER_SIZE = "INPUT_BUFFER_SIZE";
    static final String OUTPUT_BUFFER_SIZE = "OUTPUT_BUFFER_SIZE";
    static final String SAMPLE_RATE = "SAMPLE_RATE";
    static final String RECORDING_LENGTH = "RECORDING_LENGTH";
    static final String NN_NAME = "NN_NAME";
    HashMap<String, Integer> defaultValues = new HashMap<>();

    public abstract short[] getRecordedAudioBuffer();

    abstract void initConfigData(String jsonData);

    abstract void initAudioRecord();

    abstract float[] preProcess(short[] inputBuffer);

    abstract HashMap<String, Float> postProcess(float[] floats);

    int getDataValue(String jsonData, String key) {
        try {
            JSONObject obj = new JSONObject(jsonData);
            return obj.getInt(key);
        } catch (JSONException e) {
            Log.v(LOG_TAG, "Error while reading json, " + key + " set to default value ");
        }
        return defaultValues.get(key);
    }

    String getDataString(String jsonData, String key) {
        try {
            JSONObject obj = new JSONObject(jsonData);
            return obj.getString(key);
        } catch (JSONException e) {
            Log.v(LOG_TAG, "Error while reading json, " + key + " set to default value ");
        }
        return "";
    }

    ArrayList<String> getOutputNames(String jsonData) {
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

    @PreDestroy
    abstract void stopRecording();
}
