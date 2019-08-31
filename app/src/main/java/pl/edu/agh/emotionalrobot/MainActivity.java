package pl.edu.agh.emotionalrobot;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Collections;

import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public class MainActivity extends AppCompatActivity {

    EmotionDataGatherer emotionDataGatherer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        emotionDataGatherer = new EmotionDataGatherer(Collections.<EmotionRecognizer>emptyList());

        Button button = (Button) findViewById(R.id.button);
        final EditText intervalText = (EditText) findViewById(R.id.interval);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int interval = Integer.getInteger(intervalText.getText().toString());
                    UpdateSender.Options options = new UpdateSender.Options(interval);
                    UpdateSender updateSender = new UpdateSender(options);
                    //TODO put an animation on top of everything
                    emotionDataGatherer.startGatheringEmotions(updateSender);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Fill the interval", Toast.LENGTH_SHORT);
                }
            }
        });

    }
}
