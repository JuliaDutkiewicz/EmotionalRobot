package pl.edu.agh.emotionalrobot.recognizers.audio;

import android.util.Log;
import java.nio.MappedByteBuffer;
import java.util.HashMap;


import pl.edu.agh.emotionalrobot.recognizers.audio.utils.LibrosaMFCC;

public class AudioEmotionRecognizer extends AbstractAudioEmotionRecognizer {
    private static final String LOG_TAG = AbstractAudioEmotionRecognizer.class.getSimpleName();


    public AudioEmotionRecognizer(MappedByteBuffer modelFile, String jsonData) {
        super(modelFile, jsonData);
    }

    @Override
    public String getName() {
        return this.getNnName();
    }

    @Override
    float[] preProcess(short[] inputBuffer) {
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
        for (int i = 0; i < this.getOutputBufferSize(); i++) {
            results.put(this.getOutputNames().get(i), floats[i]);
            resultsText.append(this.getOutputNames().get(i)).append(": ").append(floats[i]).append("\n");
        }
        Log.v(LOG_TAG, resultsText.toString());
        return results;
    }
}
