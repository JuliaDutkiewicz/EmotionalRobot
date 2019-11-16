package pl.edu.agh.emotionalrobot.recognizers.video;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

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

//            savePicture(bitmapImage);

            synchronized (semaphore) {
                currentImage = bitmapImage;
                semaphore.notify();
            }
        }

        private void savePicture(Bitmap bitmapImage) {
            Log.d("Camera1", "Picture taken");
            File pictureFileDir = getDir();

            if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

                Log.d("Camera1", "Can't create directory to save image.");
                Toast.makeText(context, "Can't create directory to save image.",
                        Toast.LENGTH_LONG).show();
                return;

            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
            String date = dateFormat.format(new Date());
            String photoFile = "Picture_" + date + ".jpg";

            String filename = pictureFileDir.getPath() + File.separator + photoFile;

            File pictureFile = new File(filename);

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                Toast.makeText(context, "New Image saved:" + photoFile,
                        Toast.LENGTH_LONG).show();
            } catch (Exception error) {
                Log.d("Camera1", "File" + filename + "not saved: "
                        + error.getMessage());
                Toast.makeText(context, "Image could not be saved.",
                        Toast.LENGTH_LONG).show();
            }

        }

        private File getDir() {
            File sdDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            return new File(sdDir, "CameraAPIDemo");
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
            Camera.Parameters params = camera.getParameters();
//            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//                params.set("orientation", "portrait");
//                params.set("rotation", 90);
//            }
//            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                params.set("orientation", "landscape");
//                params.set("rotation", 0);
//            }
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
