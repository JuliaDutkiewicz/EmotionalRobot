package pl.edu.agh.emotionalrobot.recognizers.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Objects;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractAudioEmotionRecognizerTest {
    @Mock
    Microphone microphone;
    private MappedByteBuffer model = loadModelFile("audio_model.tflite");
    private String json = "{\n" +
            "  \"EMOTIONS\": [\n" +
            "    \"female_angry\",\n" +
            "    \"female_calm\",\n" +
            "    \"female_fearful\",\n" +
            "    \"female_happy\",\n" +
            "    \"female_sad\",\n" +
            "    \"male_angry\",\n" +
            "    \"male_calm\",\n" +
            "    \"male_fearful\",\n" +
            "    \"male_happy\",\n" +
            "    \"male_sad\"\n" +
            "  ],\n" +
            "  \"MODEL_FILE\": \"audio_model.tflite\",\n" +
            "  \"SAMPLE_RATE\": 44100,\n" +
            "  \"RECORDING_LENGTH\": 44100,\n" +
            "  \"INPUT_BUFFER_SIZE\": 216,\n" +
            "  \"NN_NAME\": \"Speech-Emotion-Analyzer\"\n" +
            "}";
    private AudioEmotionRecognizer audioEmotionRecognizer = new AudioEmotionRecognizer(model, json);


    public AbstractAudioEmotionRecognizerTest() throws Exception {

    }


    private MappedByteBuffer loadModelFile(String fileName) throws IOException {
        AssetFileDescriptor fileDescriptor = InstrumentationRegistry.getTargetContext().getAssets().openFd(fileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void mockMicrophone(String file_name) throws IOException {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream testInput = testContext.getAssets().open(file_name);
        byte[] sound = readWAVAudioFileData(testInput);
        short[] shorts = new short[sound.length/2];
        ByteBuffer.wrap(sound).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        when(microphone.getRecordedAudioBuffer(44100)).thenReturn(shorts);
        try {
            FieldSetter.setField(audioEmotionRecognizer, Objects.requireNonNull(audioEmotionRecognizer.getClass().getSuperclass()).getDeclaredField("microphone"), microphone);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private byte[] readWAVAudioFileData(InputStream testInput) {
        byte[] buffer = new byte[44100];
        try {
            int outputSize = testInput.read(buffer);
            System.out.println(outputSize);
        } catch (IOException e) {
            System.out.println("liiiiipa");
            e.printStackTrace();
        }
        return buffer;
    }


    private String getBestEmotion() {
        Map<String, Float> emotions = audioEmotionRecognizer.getEmotions();
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
    public void testGetEmotionsFemaleFearful() throws IOException {
        mockMicrophone("1575715431female_fearful.wav");
        assertEquals("female_fearful", getBestEmotion());
    }
    @Test
    public void testGetEmotionsFemaleSad() throws IOException {
        mockMicrophone("1575715485female_sad.wav");
        assertEquals("female_sad", getBestEmotion());
    }
    @Test
    public void testGetEmotionsMaleSad() throws IOException {
        mockMicrophone("1575715584male_sad.wav");
        assertEquals("male_sad", getBestEmotion());
    }
    @Test
    public void testGetEmotionsMaleFearful() throws IOException {
        mockMicrophone("1575717697male_fearful.wav");
        assertEquals("male_fearful", getBestEmotion());
    }
    @Test
    public void testGetEmotionsFemaleCalm() throws IOException {
        mockMicrophone("1575717749female_calm.wav");
        assertEquals("female_calm", getBestEmotion());
    }

    @Test
    public void testGetEmotionsFemaleAngry() throws IOException {
        mockMicrophone("1575718318female_angry.wav");
        assertEquals("female_angry", getBestEmotion());
    }

    @Test
    public void testGetEmotionsFemaleHappy() throws IOException {
        mockMicrophone("1575746482female_happy.wav");
        assertEquals("female_happy", getBestEmotion());
    }

    @Test
    public void testGetEmotionsMaleHappy() throws IOException {
        mockMicrophone("1575746637male_happy.wav");
        assertEquals("male_happy", getBestEmotion());
    }

    @Test
    public void testGetEmotionsMaleAngry() throws IOException {
        mockMicrophone("1575747331male_angry.wav");
        assertEquals("male_angry", getBestEmotion());
    }
}