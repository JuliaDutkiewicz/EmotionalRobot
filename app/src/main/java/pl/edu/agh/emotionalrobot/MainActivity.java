package pl.edu.agh.emotionalrobot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("tensorflow_inference");
    }

    private static final String MODEL_FILE_VIDEO = "file:///android_asset/optimized_tfdroid.pb";
    private static final String MODEL_FILE_AUDIO = "file:///android_asset/optimized_saved_model_v3.pb";
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
//                    inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE_VIDEO);
//                    float[] inputFloats = {Float.parseFloat(num1.getText().toString()),
//                            Float.parseFloat(num2.getText().toString()),
//                            Float.parseFloat(num3.getText().toString())};
//                    inferenceInterface.feed(INPUT_NODE, inputFloats, INPUT_SIZE);
//                    inferenceInterface.run(new String[]{OUTPUT_NODE});
//
//                    float[] result = {0, 0};
//                    inferenceInterface.fetch(OUTPUT_NODE, result);
                    final TextView textViewR = (TextView) findViewById(R.id.textView);

//                    JSONObject word_dict = new JSONObject(loadJSONFromAsset());
//                    Iterator<String> iter = word_dict.keys();
//                    HashMap<String, Integer> tokenizer = new HashMap<>();
//                    while (iter.hasNext()) {
//                        try {
//                            String key = iter.next();
//                            Integer value = word_dict.getInt(key);
//                            textViewR.setText(key + " " + value.toString());
//                            tokenizer.put(key, value);
//                        } catch (JSONException ee) {
//                            ee.printStackTrace();
//                        }
//                    }
//
//                    textViewR.setText(tokenizer.size());


                    inferenceForAudio = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE_AUDIO);

                    textViewR.setText("retertrertetr");
                    int[] inputChars = {Integer.parseInt(num1.getText().toString()),
                            Integer.parseInt(num2.getText().toString()),
                            Integer.parseInt(num3.getText().toString())};
                    inferenceForAudio.feed("input_1", inputChars, INPUT_SIZE);
                    inferenceForAudio.run(new String[]{OUTPUT_NODE});



                    float[] outputChar = {0};
                    inferenceForAudio.fetch("output_node0", outputChar);
                    textViewR.setText(Arrays.toString(outputChar));

                } catch (Exception e) {
                    final TextView errorTextR = (TextView) findViewById(R.id.errorText);
                    errorTextR.setText(e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }


    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream inputStream = getAssets().open("word_dict.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

}
