package pl.edu.agh.emotionalrobot;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import pl.edu.agh.emotionalrobot.recognizers.AudioEmotionRecognizer;
import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public class EmotionService {
    private static final String DEFAULT_AUDIO_MODEL_NAME = "audio_model.tflite";
    private static final String AUDIO_CONFIG_FILE = "audio.json";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int DEFAULT_INTERVAL = 5000;
    private ArrayList<EmotionRecognizer> emotionRecognizers;

    private final Context context;

    private EmotionDataGatherer emotionDataGatherer;

    public EmotionService(Context context) {
        this.context = context;
    }

    public void start() {
        this.emotionRecognizers = new ArrayList<>();
        loadAudioRecognizerFromConfig();

        emotionDataGatherer = new EmotionDataGatherer(emotionRecognizers);
        try {
            int interval = DEFAULT_INTERVAL;
            EmotionDataGatherer.Options options = new EmotionDataGatherer.Options(interval);
            UpdateSender updateSender = new UpdateSender(context);
            //TODO put an animation on top of everything
            emotionDataGatherer.startGatheringEmotions(updateSender, options);
        } catch (Exception e) {
            Log.v(LOG_TAG, e.getMessage());
            Toast.makeText(context, "Error while emotion gathering.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadAudioRecognizerFromConfig() {
        try {
            String configJson = loadJSONFromAsset(AUDIO_CONFIG_FILE);
            String audioModelName = getAudioModelName(configJson);
            AudioEmotionRecognizer audioEmotionRecognizer = new AudioEmotionRecognizer(loadModelFile(audioModelName), configJson);
            emotionRecognizers.add(audioEmotionRecognizer);
        } catch (IOException e) {
            Log.v(LOG_TAG, "Error by loading audio model. " + e.getMessage());
        }
    }

    private MappedByteBuffer loadModelFile(String fileName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(fileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private String getAudioModelName(String jsonData) {
        try {
            JSONObject obj = new JSONObject(jsonData);
            return obj.getString("DEFAULT_AUDIO_MODEL_NAME");
        } catch (JSONException e) {
            Log.v(LOG_TAG, "Error while reading json, DEFAULT_AUDIO_MODEL_NAME name set to default value");
        }
        return DEFAULT_AUDIO_MODEL_NAME;
    }
}
