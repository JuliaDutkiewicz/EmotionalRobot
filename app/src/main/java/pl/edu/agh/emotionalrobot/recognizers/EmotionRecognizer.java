package pl.edu.agh.emotionalrobot.recognizers;

import java.nio.ByteBuffer;
import java.util.Map;

public interface EmotionRecognizer {
    public Map<String, Float> getEmotions();

    public String getName();

    public ByteBuffer getRawData();
}
