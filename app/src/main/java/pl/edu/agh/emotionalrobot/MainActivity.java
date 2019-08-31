package pl.edu.agh.emotionalrobot;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class MainActivity extends AppCompatActivity {

    private static final String AUDIO_MODEL = "audio_converted_model.tflite";
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_RECORD_AUDIO = 13;
    private SpeechHelper speechHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = (Button) findViewById(R.id.button2);
        final Button audioButton = (Button) findViewById(R.id.button);
        final Button stopAudioRecordingButton = (Button) findViewById(R.id.button3);
        button.setOnClickListener(new View.OnClickListener() {
                                      @SuppressLint("SetTextI18n")
                                      @Override
                                      public void onClick(View v) {
                                          try {
                                              Log.v(LOG_TAG, "Start video");
                                              Interpreter interpreter = new Interpreter(loadModelFile("converted_model.tflite"));
                                              float[][][][] input = preproscessImage(R.drawable.happy_face);
                                              float[][] output = new float[1][7];
                                              interpreter.run(input, output);
                                              final TextView textViewR = (TextView) findViewById(R.id.textView);
                                              textViewR.setText("angry " + Float.toString(output[0][0])
                                                      + "\ndisgust " + Float.toString(output[0][1])
                                                      + "\nfear " + Float.toString(output[0][2])
                                                      + "\nhappy " + Float.toString(output[0][3])
                                                      + "\nsad " + Float.toString(output[0][4])
                                                      + "\nsuprise " + Float.toString(output[0][5])
                                                      + "\nneutral " + Float.toString(output[0][6]));
                                          } catch (Exception e) {
                                              final TextView textViewR = (TextView) findViewById(R.id.textView);
                                              textViewR.setText(e.getMessage());
                                          }
                                      }
                                  }

        );
        audioButton.setOnClickListener(new View.OnClickListener() {
                                      @SuppressLint("SetTextI18n")
                                      @Override
                                      public void onClick(View v) {
                                          try {

                                              Log.v(LOG_TAG, "Start audio");
                                              Interpreter audioInterpreter = new Interpreter(loadModelFile(AUDIO_MODEL));
                                              final TextView textViewR = (TextView) findViewById(R.id.textView);
                                              speechHelper = new SpeechHelper(audioInterpreter);
                                              requestMicrophonePermission();
                                          } catch (Exception e) {
                                              final TextView textViewR = (TextView) findViewById(R.id.textView);
                                              textViewR.setText(e.getMessage());
                                          }
                                      }
                                  }

        );
        stopAudioRecordingButton.setOnClickListener(new View.OnClickListener() {
                                      @SuppressLint("SetTextI18n")
                                      @Override
                                      public void onClick(View v) {
                                          try {

                                              Log.v(LOG_TAG, "Stop audio");
                                              speechHelper.shouldContinue = false;
                                              speechHelper.shouldContinueRecognition = false;
                                          } catch (Exception e) {
                                              final TextView textViewR = (TextView) findViewById(R.id.textView);
                                              textViewR.setText(e.getMessage());
                                          }
                                      }
                                  }

        );

    }

    private void requestMicrophonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            speechHelper.startRecording();
            speechHelper.startRecognition();
        }
    }

    private float[][][][] preproscessImage(int picture) {
        Bitmap bmp = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                picture);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmp, 64, 64, false);
        float[][][][] result = new float[1][64][64][1];
        for (int i = 0; i < 64; i++)
            for (int j = 0; j < 64; j++) {
                int pixel = scaledBitmap.getPixel(i, j);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                float gray = (float) Math.round(r * 0.299 + g * 0.587 + b * 0.114);
                gray = (float) (gray / 255.0);
                gray = (float) (gray - 0.5);
                gray = (float) (gray * 2.0);
                result[0][j][i][0] = gray;
            }
        return result;
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

