package pl.edu.agh.emotionalrobot.recognizers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class VideoEmotionRecognizer implements EmotionRecognizer {

    private static final String TAG = "VideoEmotionRecognizer";

    private final int screenRotation;
    private final Object semaphore = new Object();
    Surface surface;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSessions;
    private FaceDetector faceDetector;
    private ImageReader reader;
    private CameraDevice cameraDevice;
    private Size imageDimension;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private CameraManager cameraManager;
    private Bitmap currentImage;
    private Context applicationContext;
    private Interpreter interpreter;
    private String cameraId;
    private SurfaceTexture texture;
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    private LinkedList<String> emotionNames;

    public VideoEmotionRecognizer(Context context, int screenRotation, CameraManager cameraManager, MappedByteBuffer model, String config) throws Exception {
        this.applicationContext = context;
        this.screenRotation = screenRotation;
        this.cameraManager = cameraManager;
        this.interpreter = new Interpreter(model);
        this.emotionNames = getEmotionNames(config);
        this.faceDetector = new
                FaceDetector.Builder(applicationContext).setTrackingEnabled(false)
                .build();
        openCamera(this.cameraManager);
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
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

    private void openCamera(CameraManager manager) throws Exception {

        try {
            for (String camera : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(camera);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = camera;
                }
            }
            if (cameraId == null) {
                cameraId = manager.getCameraIdList()[0];
            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Check  permission for camera
            if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "VideoEmotionRecognizer needs permissions to use camera");
                throw new Exception("VideoEmotionRecognizer needs permissions to use camera");
            }
            manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Float> getEmotions() {
        Map<String, Float> emotions = new HashMap<>();
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
        Bitmap rotatedImage = rotateBitMap(currentImage);
        Bitmap face = getFace(rotatedImage);
        if (face == null) {
            emotions.put("no_face", (float) 1.0);
            return emotions;
        }

        float[][][][] input = preproscessImage(face);
        float[][] output = new float[1][7];
        interpreter.run(input, output);

        for(int i =0; i< emotionNames.size(); i++){
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

    private Bitmap rotateBitMap(Bitmap image) {
        Matrix matrix = new Matrix();
        switch (screenRotation) {
            case 0:
                matrix.postRotate(270);
                break;
            case 1:
                matrix.postRotate(0);
                break;
            case 3:
                matrix.postRotate(180);
                break;
            case 4:
                matrix.postRotate(90);
                break;
        }
        return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);

    }

    private void takePicture() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) applicationContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 480;
            int height = 640;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
            List<Surface> outputSurfaces = new ArrayList<Surface>(1);
            outputSurfaces.add(reader.getSurface());
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                    synchronized (semaphore) {
                        currentImage = bitmapImage;
                        semaphore.notify();
                    }
                    image.close();


                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {

                    super.onCaptureCompleted(session, request, result);
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            texture = new SurfaceTexture(1);
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "Configuration change");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, null);
            Log.d(TAG, "DEBUG1");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}
