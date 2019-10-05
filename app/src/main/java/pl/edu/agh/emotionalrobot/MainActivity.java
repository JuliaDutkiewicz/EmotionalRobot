package pl.edu.agh.emotionalrobot;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EmotionService emotionService = new EmotionService(getApplication());
        emotionService.start();
    }

}
