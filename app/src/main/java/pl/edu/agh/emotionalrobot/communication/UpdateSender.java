package pl.edu.agh.emotionalrobot.communication;

import android.content.Context;
import android.util.Pair;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;

import java.util.Date;
import java.util.Map;

public class UpdateSender {
    private final CommunicationConfig config;
    private final Context context;
    private boolean initialized = false;
    private MqttAndroidClient client;

    public UpdateSender(Context context, CommunicationConfig config) {
        this.context = context;
        this.config = config;
    }

    public void initialize() {
        String clientId = MqttClient.generateClientId();
        String serverURI = String.format("%s://%s:%s", config.BROKER_PROTOCOL, config.BROKER_IP_OR_NAME, config.BROKER_PORT);
        client = new MqttAndroidClient(context, serverURI, clientId, new MemoryPersistence(), MqttAndroidClient.Ack.AUTO_ACK);
        initialized = true;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean sendUpdate(final Date timestamp, final byte[] rawData, final String recognizerName) {
        try {
            send(Formatter.formatRawData(timestamp, rawData, recognizerName));
            return true;
        } catch (MqttException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void send(String message) throws MqttException {
        ActionListener callback = new ActionListener(client, message, config.UPDATE_TOPIC);
        client.connect(null, callback);
    }

    public boolean sendUpdate(final Date timestamp, final Map<String, Float> emotionData, String recognizerName) {
        try {
            send(Formatter.formatEmotionData(timestamp, emotionData, recognizerName));
            return true;
        } catch (MqttException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendUpdate(final Date timestamp, final Pair<Map<String, Float>, byte[]> emotionWithRawData, String recognizerName) {
        try {
            send(Formatter.formatEmotionWithRawData(timestamp, emotionWithRawData, recognizerName));
            return true;
        } catch (MqttException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static class ActionListener implements IMqttActionListener {
        private final MqttAndroidClient client;
        private final String message;
        private final String topic;

        ActionListener(MqttAndroidClient client, String message, String topic) {
            this.client = client;
            this.message = message;
            this.topic = topic;
        }

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            try {
                System.out.println(message);
                client.publish(topic, new MqttMessage(message.getBytes()));
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            System.out.println("Not connected :/");
            exception.printStackTrace();
        }
    }
}
