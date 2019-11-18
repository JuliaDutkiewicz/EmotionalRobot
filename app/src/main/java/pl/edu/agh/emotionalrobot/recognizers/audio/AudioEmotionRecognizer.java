package pl.edu.agh.emotionalrobot.recognizers.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Pair;

import org.tensorflow.lite.Interpreter;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;

import pl.edu.agh.emotionalrobot.recognizers.audio.utils.LibrosaMFCC;
import pl.edu.agh.emotionalrobot.recognizers.audio.utils.TarsosMFCC;

public class AudioEmotionRecognizer extends AbstractAudioEmotionRecognizer {
    private static final String LOG_TAG = AbstractAudioEmotionRecognizer.class.getSimpleName();

    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_RECORDING_LENGTH = 44280;
    // minimal buffer length for sample rate 16000 Hz is 1280 => 1296
    // minimal buffer length for sample rate 44100 Hz is 3584 => 3672 - this configuration is recommended
    // because 44100 Hz was the sample rate for training the current neural network

    // neural network size
    private static final int DEFAULT_INPUT_BUFFER_SIZE = 216;
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 10; // warning: must be equals outputNames.size()
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private int sampleRate;
    private int inputBufferSize;
    private int outputBufferSize; // warning: must be equals outputNames.size()
    private int recordingLength;
    private short[] recordingBuffer;
    private int recordingOffset = 0;
    private short[] audioBuffer;
    private AudioRecord audioRecord;
    private Interpreter interpreter;
    private ArrayList<String> outputNames;

    public AudioEmotionRecognizer(MappedByteBuffer modelFile, String jsonData) {
        this.interpreter = new Interpreter(modelFile);
        initConfigData(jsonData);
        initAudioRecord();
    }

    public static double[] normalize(double[] arr) {
        double minn = Double.MAX_VALUE;
        double maxn = Double.MIN_VALUE;
        for (int i = 0; i < arr.length; i++) {
            maxn = Math.max(arr[i], maxn);
            minn = Math.min(arr[i], minn);
        }
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (arr[i] - minn) / (maxn - minn);
        }
        return arr;
    }

    @Override
    void initConfigData(String jsonData) {
        this.outputNames = getOutputNames(jsonData);
        this.defaultValues.put(INPUT_BUFFER_SIZE, DEFAULT_INPUT_BUFFER_SIZE);
        this.defaultValues.put(OUTPUT_BUFFER_SIZE, DEFAULT_OUTPUT_BUFFER_SIZE);
        this.defaultValues.put(SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
        this.defaultValues.put(RECORDING_LENGTH, DEFAULT_RECORDING_LENGTH);
        this.inputBufferSize = getDataValue(jsonData, INPUT_BUFFER_SIZE);
        this.outputBufferSize = getDataValue(jsonData, OUTPUT_BUFFER_SIZE);
        this.sampleRate = getDataValue(jsonData, SAMPLE_RATE);
        this.recordingLength = getDataValue(jsonData, RECORDING_LENGTH);
        this.recordingBuffer = new short[recordingLength];
    }

    @Override
    void initAudioRecord() {
        int bufferSize = getBufferSize();
        this.audioBuffer = new short[bufferSize / 2];
        this.audioRecord =
                new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            initAudioRecord();
        }

        audioRecord.startRecording();
    }

    @Override
    public Map<String, Float> getEmotions() {
        record();
        short[] inputBuffer = getRecordedAudioBuffer();
        return recognize(inputBuffer);
    }

    @Override
    public byte[] getRawData() {
        record();
        short[] audioBuffer = getRecordedAudioBuffer();
        return extractRawData(audioBuffer);
    }

    private byte[] extractRawData(short[] audioBuffer) {
        byte[] byteAudioBuffer = new byte[audioBuffer.length * 2];

        int i = 0;
        for (short x : audioBuffer) {
            byteAudioBuffer[i] = (byte) (x & 0x00FF);
            byteAudioBuffer[i + 1] = (byte) ((x & 0xFF00) >> 8);
            i += 2;
        }
        String str = new String(byteAudioBuffer);
        str = str.replaceAll("\n", "");
        return str.getBytes();
    }

    @Override
    public Pair<Map<String, Float>, byte[]> getEmotionsWithRawData() {
        record();
        short[] audioBuffer = getRecordedAudioBuffer();
        Map<String, Float> emotions = recognize(audioBuffer);
        byte[] rawData = extractRawData(audioBuffer);
        return new Pair<>(emotions, rawData);
    }

    public short[] getRecordedAudioBuffer() {
        short[] inputBuffer = new short[recordingLength];

        recordingBufferLock.lock();
        try {
            int maxLength = recordingBuffer.length;
            int firstCopyLength = maxLength - recordingOffset;
            int secondCopyLength = recordingOffset;
            System.arraycopy(recordingBuffer, recordingOffset, inputBuffer, 0, firstCopyLength);
            System.arraycopy(recordingBuffer, 0, inputBuffer, firstCopyLength, secondCopyLength);
        } finally {
            recordingBufferLock.unlock();
        }
        return inputBuffer;
    }

    @Override
    public String getName() {
        return "audio";
    }

    private void record() {
        int numberRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
        int maxLength = recordingBuffer.length;
        int newRecordingOffset = recordingOffset + numberRead;
        int secondCopyLength = Math.max(0, newRecordingOffset - maxLength);
        int firstCopyLength = numberRead - secondCopyLength;

        recordingBufferLock.lock();
        try {
            System.arraycopy(audioBuffer, 0, recordingBuffer, recordingOffset, firstCopyLength);
            System.arraycopy(audioBuffer, firstCopyLength, recordingBuffer, 0, secondCopyLength);
            recordingOffset = newRecordingOffset % maxLength;
        } finally {
            recordingBufferLock.unlock();
        }
    }

    private int getBufferSize() {
        int bufferSize =
                AudioRecord.getMinBufferSize(
                        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = sampleRate * 2;
        }
        return bufferSize;
    }

    private Map<String, Float> recognize(short[] inputBuffer) {

//    //    LibrosaMFCC
//        float[] mfccInput = preProcess(inputBuffer);
//        float[][] outputFull = new float[1][outputBufferSize];
//        for (int j = 0; j < outputBufferSize; j++) {
//            outputFull[0][j] += 0;
//        }
//        int REPEATS_TIMES = (int) mfccInput.length / inputBufferSize;
//        if (REPEATS_TIMES != 0) {
//            for (int k = 0; k < REPEATS_TIMES; k++) {
//                float[] floatInputBuffer = getRequiredSizeFrame(mfccInput, k);
//                float[][] output = new float[1][outputBufferSize];
//                interpreter.run(floatInputBuffer, output);
//                for (int j = 0; j < outputBufferSize; j++) {
//                    outputFull[0][j] += output[0][j];
//                }
//            }
//            for (int j = 0; j < outputBufferSize; j++) {
//                outputFull[0][j] /= REPEATS_TIMES;
//            }
//        }
//        return postProcess(output[0]);

        float[][] outputFull = new float[1][outputBufferSize];
        for (int j = 0; j < outputBufferSize; j++) {
            outputFull[0][j] += 0;
        }
//                float[] mfccInput = preProcess(inputBuffer);
//        float[][] output = new float[1][outputBufferSize];
//        interpreter.run(mfccInput, output);
//        return postProcess(output[0]);
        float[] mfccInput = preProcessing(inputBuffer);
        int REPEATS_TIMES = (int) mfccInput.length / inputBufferSize;
        for (int k = 0; k < REPEATS_TIMES; k++) {
            float[] floatInputBuffer = getRequiredSizeFrame(mfccInput, k);
            float[][] output = new float[1][outputBufferSize];
            interpreter.run(floatInputBuffer, output);
            for (int j = 0; j < outputBufferSize; j++) {
                outputFull[0][j] += output[0][j];
            }
        }
        for (int j = 0; j < outputBufferSize; j++) {
            outputFull[0][j] /= REPEATS_TIMES;
        }
        return postProcessing(outputFull[0]);
    }

    // process recorded audio to buffer required by neural network
    private float[] getRequiredSizeFrame(float[] mfccInput, int k) {
        float[] floatInputBuffer = new float[inputBufferSize];
        for (int i = 0; i < inputBufferSize; i++) {
            floatInputBuffer[i] = (float) mfccInput[(i * k + (inputBufferSize)) % mfccInput.length];
        }
        return floatInputBuffer;
    }

    @Override
    float[] preProcessing(short[] inputBuffer) {
//        Librosa MFCC
//        double[] doubleInputBuffer = new double[inputBuffer.length];
//        LibrosaMFCC mfccConvert = new LibrosaMFCC();
//        for (int i = 0; i < inputBuffer.length; i++) {
//            doubleInputBuffer[i] = (double) (inputBuffer[i] / 1.0);
//        }
//
//        return mfccConvert.process(doubleInputBuffer);

        // TarsosMFCC
        float[] floatInputBuffer = new float[inputBuffer.length];
        TarsosMFCC mfccConvert = new TarsosMFCC(DEFAULT_RECORDING_LENGTH, sampleRate);
        for (int i = 0; i < inputBuffer.length; i++) {
            floatInputBuffer[i] = (float) (inputBuffer[i] / 32767.0f);
//            System.out.println(floatInputBuffer[i]);
        }
        return mfccConvert.process(floatInputBuffer);
    }

    @Override
    HashMap<String, Float> postProcessing(float[] floats) {
        HashMap<String, Float> results = new HashMap<>();
        StringBuilder resultsText = new StringBuilder("Results: \n");
        for (int i = 0; i < outputBufferSize; i++) {
            results.put(this.outputNames.get(i), floats[i]);
            resultsText.append(outputNames.get(i)).append(": ").append(floats[i]).append("\n");
        }
        Log.v(LOG_TAG, resultsText.toString());
        return results;
    }

    @PreDestroy
    public void stopRecording() {
        audioRecord.stop();
    }
}
