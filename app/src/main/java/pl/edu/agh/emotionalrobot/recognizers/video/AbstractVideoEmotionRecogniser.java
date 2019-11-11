package pl.edu.agh.emotionalrobot.recognizers.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public abstract class AbstractVideoEmotionRecogniser implements EmotionRecognizer {
    private static final String TAG = "VideoEmotionRecognizer";
    private FaceDetector faceDetector;
    private Interpreter interpreter;
    private Camera camera;
    private Context applicationContext;

    private LinkedList<String> emotionNames;

    public AbstractVideoEmotionRecogniser(Context context, int screenRotation, CameraManager cameraManager, MappedByteBuffer model, String config) throws Exception {
        this.interpreter = new Interpreter(model);
        this.emotionNames = getEmotionNames(config);
        this.faceDetector = new
                FaceDetector.Builder(context).setTrackingEnabled(false)
                .build();
        this.camera = new Camera(context, screenRotation, cameraManager);
        this.applicationContext = context;
    }

    private LinkedList<String> getEmotionNames(String config) {
        LinkedList<String> emotionNames = new LinkedList<>();
        try {
            JSONObject obj = new JSONObject(config);
            JSONArray names = obj.getJSONArray("emotions");

            for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                emotionNames.add(name);
            }
        } catch (JSONException e) {
            Log.v(TAG, "Error while reading json");
        }
        return emotionNames;
    }

    @Override
    public Map<String, Float> getEmotions() {
        Map<String, Float> emotions = new HashMap<>();
        Bitmap image = camera.getPicture();
        Bitmap face = getFace(image);
        if (face == null) {
            emotions.put("no_face", (float) 1.0);
            return emotions;
        }

        float[][][][] input = preproscessImage(face);
        float[][] output = new float[1][7];
        interpreter.run(input, output);

        for (int i = 0; i < emotionNames.size(); i++) {
            emotions.put(emotionNames.get(i), output[0][i]);
        }
        Log.i("VideoEmotionRecognizer", emotions.toString());
        return emotions;
    }


    private Bitmap getFace(Bitmap bmp) {
        if (!faceDetector.isOperational()) {
            new AlertDialog.Builder(applicationContext).setMessage("Could not set up the face detector!").show();
            return null;
        }

        Frame frame = new Frame.Builder().setBitmap(bmp).build();
        SparseArray<Face> faces = faceDetector.detect(frame);
        if (faces.size() == 0) {
            Log.e(TAG, "No face recogized");
            return null;
        }
        Face face = faces.valueAt(0);
        float x1 = face.getPosition().x;
        float y1 = face.getPosition().y;
        float width = face.getWidth();
        float height = face.getHeight();
        Bitmap tempBitmap = Bitmap.createBitmap(bmp, (int) x1, (int) y1, Math.min((int) width,
                bmp.getWidth() - (int) x1), Math.min((int) height, bmp.getHeight() - (int) y1));
        return tempBitmap;
    }

    protected abstract float[][][][] preproscessImage(Bitmap bmp);
}
