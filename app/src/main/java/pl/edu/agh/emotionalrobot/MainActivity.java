package pl.edu.agh.emotionalrobot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("tensorflow_inference");
    }

    private static final String MODEL_FILE_VIDEO = "file:///android_asset/optimized_tfdroid.pb";
    private static final String MODEL_FILE_AUDIO = "file:///android_asset/audio_model.pb";
    private static final String INPUT_NODE = "I";
    private static final String OUTPUT_NODE = "O";

    private static final long[] INPUT_SIZE = {1, 3};
    private TensorFlowInferenceInterface inferenceInterface;
    private TensorFlowInferenceInterface inferenceForAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.button2);
        final EditText num1 = (EditText) findViewById(R.id.num1);
        final EditText num2 = (EditText) findViewById(R.id.num2);
        final EditText num3 = (EditText) findViewById(R.id.num3);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE_VIDEO);
                    float[] inputFloats = {Float.parseFloat(num1.getText().toString()),
                            Float.parseFloat(num2.getText().toString()),
                            Float.parseFloat(num3.getText().toString())};
                    inferenceInterface.feed(INPUT_NODE, inputFloats, INPUT_SIZE);
                    inferenceInterface.run(new String[]{OUTPUT_NODE});

                    float[] result = {0, 0};
                    inferenceInterface.fetch(OUTPUT_NODE, result);
                    final TextView textViewR = (TextView) findViewById(R.id.textView);
                    textViewR.setText(Float.toString(result[0]) + ", " + Float.toString(result[1]));

                    inferenceForAudio = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE_AUDIO);
                    byte[] inputChars = "I like to move it, move it".getBytes();
                    inferenceForAudio.feed(INPUT_NODE, inputChars, inputChars.length);
                    inferenceForAudio.run(new String[]{OUTPUT_NODE});

                    byte[] outputChar = "".getBytes();
                    inferenceForAudio.fetch(OUTPUT_NODE, outputChar);
                    textViewR.setText(Arrays.toString(outputChar));

                } catch (Exception e) {
                    final TextView textViewR = (TextView) findViewById(R.id.textView);
                    textViewR.setText(e.getMessage());//Float.toString(result[0]) + ", " + Float.toString(result[1]));
                }
            }
        });
    }
}
