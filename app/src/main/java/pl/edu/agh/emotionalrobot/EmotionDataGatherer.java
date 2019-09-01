package pl.edu.agh.emotionalrobot;

import android.util.Log;

import java.util.Collection;

import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public class EmotionDataGatherer {
    private Collection<EmotionRecognizer> emotionRecognizers;


    public EmotionDataGatherer(Collection<EmotionRecognizer> emotionRecognizers) {
        this.emotionRecognizers = emotionRecognizers;
    }


    public void startGatheringEmotions(UpdateSender updateSender) {
        for (EmotionRecognizer recognizer : emotionRecognizers) {
            recognizer.getEmotions();
        }
    }

}