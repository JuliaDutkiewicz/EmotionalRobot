package pl.edu.agh.emotionalrobot.communication;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
        client = new MqttAndroidClient(context, serverURI, clientId);
        initialized = true;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean sendUpdate(final Date timestamp, final byte[] rawData) {
        try {
            String message = formatRawData(timestamp, rawData);
            ActionListener callback = new ActionListener(client, message, config.UPDATE_TOPIC);
            client.connect(null, callback);
            return true;
        } catch (MqttException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String formatRawData(Date timestamp, byte[] rawData) throws JSONException {
        JSONObject update = new JSONObject();
        update.put("network", "nazwa sieci");
        return null;
    }

    public boolean sendUpdate(final Date timestamp, final Map<String, Float> emotionData) {
        try {
            String message = formatEmotionData(timestamp, emotionData);
            ActionListener callback = new ActionListener(client, message, config.UPDATE_TOPIC);
            client.connect(null, callback);
            return true;
        } catch (MqttException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String formatEmotionData(Date timestamp, final Map<String, Float> emotionData) throws JSONException {
        JSONObject update = new JSONObject();
        update.put("network", "nazwa sieci");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        update.put("timestamp", format.format(timestamp));
        JSONObject emotionDataObject = new JSONObject();
        for (String key : emotionData.keySet()) {
            emotionDataObject.put(key, emotionData.get(key));
        }
        update.put("emotion-data", emotionDataObject);
        JSONObject outData = new JSONObject();
        outData.put("update", update);

        return outData.toString();
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
