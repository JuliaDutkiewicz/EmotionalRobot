package pl.edu.agh.emotionalrobot;

import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import pl.edu.agh.emotionalrobot.communication.UpdateSender;
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
                        updateSender.sendUpdate(new Date(System.currentTimeMillis()), recognizer.getEmotions(), recognizer.getName());
                        updateSender.sendUpdate(new Date(System.currentTimeMillis()), recognizer.getRawData(), recognizer.getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, options.interval);
    }

    public void setUpdateInterval(int interval) {
        // TODO
        // to stop timer: apparently .cancel() & ev. .purge()
        // so if message arrives to ConfigReceiver, some sort of method like "reschedule" should be called
    }

    public static class Options {
        final int interval;

        public Options(int interval) {
            this.interval = interval;
        }
    }
}
