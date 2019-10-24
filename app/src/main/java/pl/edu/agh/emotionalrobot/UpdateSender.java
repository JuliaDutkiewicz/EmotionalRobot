package pl.edu.agh.emotionalrobot;

import android.content.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

class UpdateSender {
    private Context context;
    private boolean initialized = false;
    private MqttAndroidClient client;

    public UpdateSender(Context context) {
        this.context = context;
    }

    public void initialize() {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(context, "tcp://broker.hivemq.com:1883",
                clientId);
        initialized = true;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public boolean sendUpdate(final Date timestamp, final Map<String, Float> emotionData) {
        try {
            client.connect(null, new ActionListener(client, formatData(timestamp, emotionData)));
            return true;
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String formatData(Date timestamp, Map<String, Float> emotionData) {
        StringBuilder stringBuilder = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        stringBuilder.append(format.format(timestamp));
        stringBuilder.append("\n\r");
        for (String emotion : emotionData.keySet()) {
            stringBuilder.append(" - ");
            stringBuilder.append(emotion);
            stringBuilder.append(" : ");
            stringBuilder.append(emotionData.get(emotion));
            stringBuilder.append("\n\r");
        }
        return stringBuilder.toString();
    }

    private static class ActionListener implements IMqttActionListener {
        private final MqttAndroidClient client;
        private final String message;

        ActionListener(MqttAndroidClient client, String message) {
            this.client = client;
            this.message = message;
        }

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            try {
                client.publish("emorobo.mqtt.example.topic", new MqttMessage(message.getBytes()));
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
