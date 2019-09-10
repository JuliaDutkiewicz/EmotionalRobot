package pl.edu.agh.emotionalrobot;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import pl.edu.agh.emotionalrobot.recognizers.AudioEmotionRecognizer;
import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public class MainActivity extends AppCompatActivity {

    private static final String AUDIO_MODEL = "audio_converted_model.tflite";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    EmotionDataGatherer emotionDataGatherer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = (Button) findViewById(R.id.button);
        final EditText intervalText = (EditText) findViewById(R.id.interval);

        ArrayList<EmotionRecognizer> emotionRecognizers = new ArrayList<>();
        try {
            AudioEmotionRecognizer audioEmotionRecognizer = new AudioEmotionRecognizer(loadModelFile(AUDIO_MODEL));
            emotionRecognizers.add(audioEmotionRecognizer);
        } catch (IOException e) {
            Log.v(LOG_TAG, "Error by loading audio model. " + e.getMessage());
        }

        emotionDataGatherer = new EmotionDataGatherer(emotionRecognizers);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int interval = Integer.valueOf(intervalText.getText().toString());
                    EmotionDataGatherer.Options options = new EmotionDataGatherer.Options(interval);
                    UpdateSender updateSender = new UpdateSender(getApplicationContext());
                    //TODO put an animation on top of everything
                    emotionDataGatherer.startGatheringEmotions(updateSender, options);
                } catch (Exception e) {
                    Log.v(LOG_TAG, e.getMessage());
                    Toast.makeText(getApplicationContext(), "Fill the interval", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

    private MappedByteBuffer loadModelFile(String fileName) throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd(fileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}
