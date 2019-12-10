package pl.edu.agh.emotionalrobot.recognizers;

import android.util.Pair;

import java.nio.ByteBuffer;
import java.util.Map;

public interface EmotionRecognizer {
    public Map<String, Float> getEmotions();

    public String getName();

    public byte[] getRawData();

    public Pair<Map<String, Float>, byte[]> getEmotionsWithRawData();

    public String getType();

    public void destroy();
}
