package pl.edu.agh.emotionalrobot.recognizers.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;

import java.nio.MappedByteBuffer;

public class VideoEmotionRecognizer extends AbstractVideoEmotionRecogniser {

    public VideoEmotionRecognizer(Context context, int screenRotation, CameraManager cameraManager, MappedByteBuffer model, String config) throws Exception {
        super(context, screenRotation, cameraManager, model, config);
    }

    @Override
    protected float[][][][] preprocessImage(Bitmap bmp) {
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
}
