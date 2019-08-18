package pl.edu.agh.emotionalrobot;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button = (Button) findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
                                      @SuppressLint("SetTextI18n")
                                      @Override
                                      public void onClick(View v) {
                                          try {

                                              Interpreter interpreter = new Interpreter(loadModelFile());
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


    private MappedByteBuffer loadModelFile() throws IOException {

        AssetFileDescriptor fileDescriptor = getAssets().openFd("converted_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}

