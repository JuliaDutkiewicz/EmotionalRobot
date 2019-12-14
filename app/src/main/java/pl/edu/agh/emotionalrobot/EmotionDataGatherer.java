package pl.edu.agh.emotionalrobot;

import android.util.Log;

import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import pl.edu.agh.emotionalrobot.communication.UpdateSender;
import pl.edu.agh.emotionalrobot.communication.UpdateType;
import pl.edu.agh.emotionalrobot.recognizers.EmotionRecognizer;

public class EmotionDataGatherer {
    private Collection<EmotionRecognizer> emotionRecognizers;
    private Timer timer;
    private UpdateSender updateSender;
    private Options options;
    private AtomicBoolean isSendingUpdates = new AtomicBoolean(false);
    private UpdateType updateType;

    public EmotionDataGatherer(Collection<EmotionRecognizer> emotionRecognizers, UpdateSender updateSender, Options options) {
        this.emotionRecognizers = emotionRecognizers;
        this.updateSender = updateSender;
        this.options = options;
        this.updateType = UpdateType.EMOTIONS_ONLY;
    }

    public void startGatheringEmotions(Options options) {
        this.options = options;
        startSendingUpdates();
    }

    public synchronized void startSendingUpdates() {
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
                    sendUpdate();
                } catch (Exception e) {
                    Log.v(this.getClass().getCanonicalName(), "Exception occurred when sending an update", e);
                }
            }
        }, 0, getUpdateInterval());
        isSendingUpdates.set(true);
    }

    private synchronized void sendUpdate() {
        for (EmotionRecognizer recognizer : emotionRecognizers) {
            switch (updateType) {
                case RAW_ONLY:
                    updateSender.sendUpdate(new Date(System.currentTimeMillis()), recognizer.getRawData(), recognizer.getName(), recognizer.getType());
                    break;
                case EMOTIONS_ONLY:
                    updateSender.sendUpdate(new Date(System.currentTimeMillis()), recognizer.getEmotions(), recognizer.getName(), recognizer.getType());
                    break;
                case ALL:
                    updateSender.sendUpdate(new Date(System.currentTimeMillis()), recognizer.getEmotionsWithRawData(), recognizer.getName(), recognizer.getType());
                    break;
            }
        }
    }

    public synchronized void stopSendingUpdates() {
        if (!isSendingUpdates.get() || timer == null) {
            return;
        }
        timer.cancel();
        timer.purge();
        isSendingUpdates.set(false);
    }

    private synchronized int getUpdateInterval() {
        return options.interval;
    }

    public synchronized void setUpdateInterval(int interval) {
        options.interval = interval;
    }

    public synchronized void setUpdateType(UpdateType updateType) {
        this.updateType = updateType;
    }

    public static class Options {
        int interval;

        public Options(int interval) {
            this.interval = interval;
        }
    }
}
