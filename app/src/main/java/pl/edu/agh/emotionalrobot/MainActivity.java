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

    private void openCameraActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private float[][][][] preproscessImage(Bitmap bmp) {
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

    private Bitmap getFace(Bitmap bmp) {
        FaceDetector faceDetector = new
                FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                .build();
        if (!faceDetector.isOperational()) {
            new AlertDialog.Builder(getApplicationContext()).setMessage("Could not set up the face detector!").show();
            return null;
        }
        Frame frame = new Frame.Builder().setBitmap(bmp).build();
        SparseArray<Face> faces = faceDetector.detect(frame);

        Toast.makeText(MainActivity.this, Integer.toString(faces.size()), Toast.LENGTH_SHORT).show();
        Face face = faces.valueAt(0);
        float x1 = face.getPosition().x;
        float y1 = face.getPosition().y;
        float width = face.getWidth();
        float height = face.getHeight();
        Bitmap tempBitmap = Bitmap.createBitmap(bmp, (int)x1,(int)y1, (int)width,(int) height);
        return tempBitmap;
    }
}

