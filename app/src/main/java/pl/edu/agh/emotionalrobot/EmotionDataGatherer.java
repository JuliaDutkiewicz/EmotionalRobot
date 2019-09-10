package pl.edu.agh.emotionalrobot;

import android.util.Log;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public class EmotionDataGatherer {
    private Collection<EmotionRecognizer> emotionRecognizers;


    public EmotionDataGatherer(Collection<EmotionRecognizer> emotionRecognizers) {
        this.emotionRecognizers = emotionRecognizers;
    }


    public void startGatheringEmotions(final UpdateSender updateSender, Options options) {
        if (!updateSender.isInitialized()) {
            updateSender.initialize();
        }

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    Map<String, Float> emotionData = new HashMap<>();
                    emotionData.put("sad :(", (float) 2.0);
                    emotionData.put("happy ^-^", (float) 0.2); // <==example data, should actually read each emotionRecognizer in turn
                    updateSender.sendUpdate(new Date(System.currentTimeMillis()), emotionData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, options.interval);
    }

    public static class Options {
        final int interval;

        public Options(int interval) {
            this.interval = interval;
        }
    }
}
