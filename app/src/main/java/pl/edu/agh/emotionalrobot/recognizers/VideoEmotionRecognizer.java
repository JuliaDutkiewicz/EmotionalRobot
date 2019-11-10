package pl.edu.agh.emotionalrobot.recognizers;

import java.nio.ByteBuffer;
import java.util.Map;

public class VideoEmotionRecognizer implements EmotionRecognizer {
    @Override
    public Map<String, Float> getEmotions() {
        return null;
    }

    @Override
    public ByteBuffer getRawData() {
        //TODO
        return null;
    }

    //TODO init Camera in MainActivity
}
