package pl.edu.agh.emotionalrobot;

import java.util.Collection;
import java.util.Date;
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
                    for (EmotionRecognizer recognizer : emotionRecognizers) {
                        updateSender.sendUpdate(new Date(System.currentTimeMillis()), recognizer.getEmotions());
                        updateSender.sendUpdate(new Date(System.currentTimeMillis()), recognizer.getRawData());
                    }
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
