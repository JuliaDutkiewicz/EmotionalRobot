package pl.edu.agh.emotionalrobot;

import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import pl.edu.agh.emotionalrobot.communication.UpdateSender;
import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public class EmotionDataGatherer {
    private Collection<EmotionRecognizer> emotionRecognizers;
    private Timer timer;
    private UpdateSender updateSender;
    private Options options;
    private AtomicBoolean isSendingUpdates = new AtomicBoolean(false);

    public EmotionDataGatherer(Collection<EmotionRecognizer> emotionRecognizers, UpdateSender updateSender, Options options) {
        this.emotionRecognizers = emotionRecognizers;
        this.updateSender = updateSender;
        this.options = options;
    }

    public void startGatheringEmotions(Options options) {
        this.options = options;
        startSendingUpdates();
    }

    public void startSendingUpdates() {
        if (isSendingUpdates.get()) {
            return;
        }

        if (!updateSender.isInitialized()) {
            updateSender.initialize();
        }

        timer = new Timer();
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
        isSendingUpdates.set(true);
    }

    public void stopSendingUpdates() {
        if (!isSendingUpdates.get() || timer == null) {
            return;
        }
        timer.cancel();
        timer.purge();
        isSendingUpdates.set(false);
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
