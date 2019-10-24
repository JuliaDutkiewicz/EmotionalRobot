package pl.edu.agh.emotionalrobot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import pl.edu.agh.emotionalrobot.recognizers.VideoEmotionRecognizer;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    EmotionDataGatherer emotionDataGatherer;
    VideoEmotionRecognizer videoEmotionRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(getApplicationContext(), EmotionService.class);
        getApplicationContext().startService(intent);
    }
}

