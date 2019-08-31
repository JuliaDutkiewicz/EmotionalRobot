package pl.edu.agh.emotionalrobot.recognizers;

import java.util.Map;

public interface EmotionRecognizer {
    public Map<String, Float> getEmotions();
}
