package pl.edu.agh.emotionalrobot.recognizers;

import android.util.Pair;

import java.util.Map;

public interface EmotionRecognizer {
    Map<String, Float> getEmotions();

    String getName();

    byte[] getRawData();

    Pair<Map<String, Float>, byte[]> getEmotionsWithRawData();

    String getType();
}
