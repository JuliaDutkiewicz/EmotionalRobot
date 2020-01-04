package pl.edu.agh.emotionalrobot.recognizers.video;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractVideoEmotionRecogniserTest {
    @Mock
    Camera1 camera;
    private MappedByteBuffer model = loadModelFile("video_model.tflite");
    private String json = "{\n" +
            "  \"emotions\": [\n" +
            "    \"angry\",\n" +
            "    \"disgust\",\n" +
            "    \"fear\",\n" +
            "    \"happy\",\n" +
            "    \"sad\",\n" +
            "    \"surprise\",\n" +
            "    \"neutral\"\n" +
            "  ],\n" +
            "  \"AUDIO_MODEL\": \"video_model.tflite\"\n" +
            "}";
    private VideoEmotionRecognizer videoEmotionRecognizer = new VideoEmotionRecognizer(InstrumentationRegistry.getTargetContext(), model, json, "NN");


    public AbstractVideoEmotionRecogniserTest() throws Exception {

    }


    private MappedByteBuffer loadModelFile(String fileName) throws IOException {
        AssetFileDescriptor fileDescriptor = InstrumentationRegistry.getTargetContext().getAssets().openFd(fileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void mockCamera(String file_name) throws IOException {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream testInput = testContext.getAssets().open(file_name);
        Bitmap picture = BitmapFactory.decodeStream(testInput);
        when(camera.getPicture()).thenReturn(picture);
        try {
            FieldSetter.setField(videoEmotionRecognizer, Objects.requireNonNull(videoEmotionRecognizer.getClass().getSuperclass()).getDeclaredField("camera"), camera);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private String getBestEmotion() {
        Map<String, Float> emotions = videoEmotionRecognizer.getEmotions();
        System.out.print(emotions.toString());
        String bestEmotionString = "";
        Float bestEmotionValue = (float) 0.0;
        for (Map.Entry<String, Float> entry : emotions.entrySet()) {
            if (entry.getValue() > bestEmotionValue) {
                bestEmotionString = entry.getKey();
                bestEmotionValue = entry.getValue();
            }
        }
        return bestEmotionString;
    }

    @Test
    public void testGetEmotionsAnger() throws IOException {
        mockCamera("anger.jpg");
        assertEquals("angry", getBestEmotion());
    }
    @Test
    public void testGetEmotionsSadness() throws IOException {
        mockCamera("sad.jpg");
        assertEquals("sad", getBestEmotion());
    }
    @Test
    public void testGetEmotionsFear() throws IOException {
        mockCamera("fear.jpg");
        assertEquals("fear", getBestEmotion());
    }
    @Test
    public void testGetEmotionsNeutral() throws IOException {
        mockCamera("neutral.jpg");
        assertEquals("neutral", getBestEmotion());
    }
    @Test
    public void testGetEmotionsHappiness() throws IOException {
        mockCamera("happy.jpg");
        assertEquals("happy", getBestEmotion());
    }

    @Test
    public void testGetEmotionsSuprise() throws IOException {
        mockCamera("surprise.jpg");
        assertEquals("surprise", getBestEmotion());
    }
}