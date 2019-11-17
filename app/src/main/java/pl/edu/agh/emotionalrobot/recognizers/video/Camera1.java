package pl.edu.agh.emotionalrobot.recognizers.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.Log;

public class Camera1 implements ICamera {

    private static final String TAG = "Camera1";
    private final Object semaphore = new Object();
    Integer screenRotation;
    private SurfaceTexture surfaceTexture;
    private Camera camera;
    private Context context;
    private Bitmap currentImage;
    private int cameraId;
    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Bitmap bitmapImage = BitmapFactory.decodeByteArray(data, 0, data.length, null);

            synchronized (semaphore) {
                currentImage = bitmapImage;
                semaphore.notify();
            }
        }
    };

    public Camera1(Context context, Integer screenRotation) throws Exception {
        this.screenRotation = screenRotation;
        cameraId = findFrontFacingCamera();
        if (cameraId < 0) {
            Log.e(TAG, "No front camera found");
            throw new Exception("No camera found");

        }
        this.context = context;
        try {
            camera = Camera.open(cameraId);
            camera.enableShutterSound(false);
            surfaceTexture = new SurfaceTexture(10);
            camera.setPreviewTexture(surfaceTexture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(TAG, "Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    @Override
    public Bitmap getPicture() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                takePicture();
            }
        });

        t.start();
        synchronized (semaphore) {
            try {
                semaphore.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return currentImage;
    }

    private void takePicture() {
        camera.startPreview();
        camera.takePicture(null, null, null, pictureCallback);
    }

}
