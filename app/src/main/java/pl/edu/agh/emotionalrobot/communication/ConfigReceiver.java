package pl.edu.agh.emotionalrobot.communication;

import pl.edu.agh.emotionalrobot.EmotionDataGatherer;

public class ConfigReceiver {
    private final EmotionDataGatherer emotionDataGatherer;

    public ConfigReceiver(EmotionDataGatherer emotionDataGatherer) {
        this.emotionDataGatherer = emotionDataGatherer;
        // start MQTT client with adequate callbacks
    }
}
