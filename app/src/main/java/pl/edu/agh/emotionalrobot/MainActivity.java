package pl.edu.agh.emotionalrobot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("tensorflow_inference");
    }

    private static final String MODEL_FILE = "file:///android_asset/optimized_tfdroid.pb";
    private static final String INPUT_NODE = "I";
    private static final String OUTPUT_NODE = "O";

    private static final long[] INPUT_SIZE = {1, 3};
    private TensorFlowInferenceInterface inferenceInterface;

    Button mButton;
    EditText num1;
    EditText num2;
    EditText num3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        num1 = (EditText) findViewById(R.id.num1);
        num2 = (EditText) findViewById(R.id.num2);
        num2 = (EditText) findViewById(R.id.num2);
        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
        float[] inputFloats = {Float.parseFloat(num1.getText().toString()),
                Float.parseFloat(num2.getText().toString()),
                Float.parseFloat(num3.getText().toString())};
        inferenceInterface.feed(INPUT_NODE, inputFloats, INPUT_SIZE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
