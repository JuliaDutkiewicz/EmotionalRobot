package pl.edu.agh.emotionalrobot;

import android.content.Intent;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = (Button) findViewById(R.id.button);
        final EditText intervalText = (EditText) findViewById(R.id.interval);

        final List<EmotionRecognizer> emotionRecognizers = new LinkedList<>();
        try {
            videoEmotionRecognizer = initializeVideoEmotionRecognizer();
        } catch (IOException e) {
            Toast.makeText(this, "Couldn't initialize Video EmotionRecogizer", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        emotionDataGatherer = new EmotionDataGatherer(emotionRecognizers);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int interval = Integer.valueOf(intervalText.getText().toString());
                    UpdateSender.Options options = new UpdateSender.Options(interval);
                    UpdateSender updateSender = new UpdateSender(options);
                    //TODO put an animation on top of everything
                    emotionDataGatherer.startGatheringEmotions(updateSender);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Fill the interval", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private VideoEmotionRecognizer initializeVideoEmotionRecognizer() throws IOException {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        final int rotation = getWindowManager().getDefaultDisplay().getRotation();
        VideoEmotionRecognizer recognizer = new VideoEmotionRecognizer(getApplicationContext(), rotation, manager, this);
        return recognizer;

    }

}

