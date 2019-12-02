package pl.edu.agh.emotionalrobot.recognizers.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Pair;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;

import pl.edu.agh.emotionalrobot.recognizers.audio.utils.LibrosaMFCC;
import pl.edu.agh.emotionalrobot.recognizers.audio.utils.TarsosMFCC;

public class AudioEmotionRecognizer extends AbstractAudioEmotionRecognizer {
    private static final String LOG_TAG = AbstractAudioEmotionRecognizer.class.getSimpleName();

    private static final int DEFAULT_SAMPLE_RATE = 44100;
    private static final int DEFAULT_RECORDING_LENGTH = 44100;
    // minimal buffer length for sample rate 16000 Hz is 1280 => 1296
    // minimal buffer length for sample rate 44100 Hz is 3584 => 3672 - this configuration is recommended
    // because 44100 Hz was the sample rate for training the current neural network

    // neural network size
    private static final int DEFAULT_INPUT_BUFFER_SIZE = 216;
    private static final int DEFAULT_OUTPUT_BUFFER_SIZE = 10; // warning: must be equals outputNames.size()
    private final ReentrantLock recordingBufferLock = new ReentrantLock();
    private String nnName;
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

    @Override
    void initConfigData(String jsonData) {
        this.outputNames = getOutputNames(jsonData);
        this.defaultValues.put(INPUT_BUFFER_SIZE, DEFAULT_INPUT_BUFFER_SIZE);
        this.defaultValues.put(OUTPUT_BUFFER_SIZE, DEFAULT_OUTPUT_BUFFER_SIZE);
        this.defaultValues.put(SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
        this.defaultValues.put(RECORDING_LENGTH, DEFAULT_RECORDING_LENGTH);
        this.nnName = getDataString(jsonData, NN_NAME);
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
        float[] floatAudioBuffer = shortToFloat(audioBuffer);
        return floatArray2ByteArray(floatAudioBuffer);
    }

    public static byte[] floatArray2ByteArray(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(4 * values.length);
        for (float value : values) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }


    public static float[] shortToFloat(short[] shortArray) {
        float[] floatOut = new float[shortArray.length];
        for (int i = 0; i < shortArray.length; i++) {
            floatOut[i] = shortArray[i] / 32768.0f;
        }
        return floatOut;
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
        } catch (Exception e) {
            Log.v(LOG_TAG, "Buffer warning.");
        } finally {
            recordingBufferLock.unlock();
        }
        return inputBuffer;
    }

    @Override
    public String getName() {
        return nnName;
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
        } catch (Exception e) {
            Log.v(LOG_TAG, "Buffer warning.");
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
//      LibrosaMFCC
        float[][] outputFull = new float[1][outputBufferSize];
        for (int j = 0; j < outputBufferSize; j++) {
            outputFull[0][j] += 0;
        }
        float[] mfccInput = preProcess(inputBuffer);
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
        return postProcess(outputFull[0]);

//        // TarsosMFCC
//        float[] floatInputBuffer = new float[inputBuffer.length];
//        TarsosMFCC mfccConvert = new TarsosMFCC(DEFAULT_RECORDING_LENGTH, sampleRate);
//        for (int i = 0; i < inputBuffer.length; i++) {
//            floatInputBuffer[i] = (float) (inputBuffer[i] / 32767.0f);
////            System.out.println(floatInputBuffer[i]);
//        }
//        return postProcess(mfccConvert.process(floatInputBuffer));
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
    float[] preProcess(short[] inputBuffer) {

// // TarsosMFCC
//        float[] floatInputBuffer = new float[inputBuffer.length];
//        TarsosMFCC mfccConvert = new TarsosMFCC(DEFAULT_RECORDING_LENGTH, sampleRate);
//        for (int i = 0; i < inputBuffer.length; i++) {
//            floatInputBuffer[i] = (float) (inputBuffer[i] / 32767.0f);
//        }
//        return mfccConvert.process(floatInputBuffer);

// // LibrosaMFCC
        double[] doubleInputBuffer = new double[inputBuffer.length];
        for (int i = 0; i < inputBuffer.length; i++) {
            doubleInputBuffer[i] = (inputBuffer[i] / 32768.0f);
        }
        LibrosaMFCC mfccConvert = new LibrosaMFCC();
        return mfccConvert.process(doubleInputBuffer);
    }

    @Override
    HashMap<String, Float> postProcess(float[] floats) {
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
