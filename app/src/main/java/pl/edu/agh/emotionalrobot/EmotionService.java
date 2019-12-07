package pl.edu.agh.emotionalrobot;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.os.Binder;
import android.os.IBinder;
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

import pl.edu.agh.emotionalrobot.communication.CommunicationConfig;
import pl.edu.agh.emotionalrobot.communication.UpdateSender;
import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;
import pl.edu.agh.emotionalrobot.recognizers.audio.AbstractAudioEmotionRecognizer;
import pl.edu.agh.emotionalrobot.recognizers.audio.AudioEmotionRecognizer;
import pl.edu.agh.emotionalrobot.recognizers.video.AbstractVideoEmotionRecogniser;
import pl.edu.agh.emotionalrobot.recognizers.video.VideoEmotionRecognizer;

public class EmotionService extends Service {
    private static final String DEFAULT_AUDIO_MODEL_NAME = "audio_model.tflite";
    private static final String AUDIO_CONFIG_FILE = "audio.json";
    private static final String DEFAULT_VIDEO_MODEL_NAME = "video_model.tflite";
    private static final String VIDEO_CONFIG_FILE = "video.json";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private final IBinder mBinder = new Binder();
    private ArrayList<EmotionRecognizer> emotionRecognizers;
    private EmotionDataGatherer emotionDataGatherer;

    public EmotionService() {
    }

    private AbstractAudioEmotionRecognizer loadAudioRecognizerFromConfig() throws IOException {

        String configJson = loadJSONFromAsset(AUDIO_CONFIG_FILE);
        String audioModelName = null;
        try {
            audioModelName = getModelName(configJson, "DEFAULT_AUDIO_MODEL_NAME");
        } catch (JSONException e) {
            audioModelName = DEFAULT_AUDIO_MODEL_NAME;
        }
        try {
            return new AudioEmotionRecognizer(loadModelFile(audioModelName), configJson);
        } catch (IOException e) {
            Log.v(LOG_TAG, "Error by loading audio model. " + e.getMessage());
            throw e;
        }
    }

    private AbstractVideoEmotionRecogniser loadVideoRecognizerFromConfig() throws Exception {
        String configJson = loadJSONFromAsset(VIDEO_CONFIG_FILE);
        String videoModelName = null;
        try {
            videoModelName = getModelName(configJson, "DEFAULT_VIDEO_MODEL_NAME");
        } catch (JSONException e) {
            videoModelName = DEFAULT_VIDEO_MODEL_NAME;
        }
        try {
            return new VideoEmotionRecognizer(getApplicationContext(), loadModelFile(videoModelName), configJson);
        } catch (Exception e) {
            Log.v(LOG_TAG, "Error by loading video model. " + e.getMessage());
            throw e;
        }
    }

    private MappedByteBuffer loadModelFile(String fileName) throws IOException {
        AssetFileDescriptor fileDescriptor = getApplicationContext().getAssets().openFd(fileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private String loadJSONFromAsset(String fileName) {
        String json = null;
        try {
            InputStream is = getApplicationContext().getAssets().open(fileName);
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

    private String getModelName(String jsonData, String model_key) throws JSONException {
        try {
            JSONObject obj = new JSONObject(jsonData);
            return obj.getString("DEFAULT_AUDIO_MODEL_NAME");
        } catch (JSONException e) {
            Log.v(LOG_TAG, "Error while reading json, for " + model_key + ".");
            throw e;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.emotionRecognizers = new ArrayList<>();
        try {
            emotionRecognizers.add(loadAudioRecognizerFromConfig());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Couldn't add AbstractAudioEmotionRecognizer to EmotionDataGatherer");
        }
        try {
            emotionRecognizers.add(loadVideoRecognizerFromConfig());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Couldn't add AbstractVideoEmotionRecognizer to EmotionDataGatherer");
        }

        emotionDataGatherer = new EmotionDataGatherer(emotionRecognizers);
        try {
            CommunicationConfig communicationConfig = new CommunicationConfig(loadJSONFromAsset("communication.json"));
            EmotionDataGatherer.Options options = new EmotionDataGatherer.Options(communicationConfig.STARTING_UPDATE_INTERVAL);
            UpdateSender updateSender = new UpdateSender(getApplicationContext(), communicationConfig);
            //TODO put an animation on top of everything
            Log.v(LOG_TAG, "Starting gatherer process. ");
            emotionDataGatherer.startGatheringEmotions(updateSender, options);
        } catch (Exception e) {
            Log.v(LOG_TAG, e.getMessage());
            Toast.makeText(getApplicationContext(), "Error while emotion gathering.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

}
