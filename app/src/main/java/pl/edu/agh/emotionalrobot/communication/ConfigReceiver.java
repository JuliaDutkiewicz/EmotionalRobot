package pl.edu.agh.emotionalrobot.communication;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import pl.edu.agh.emotionalrobot.EmotionDataGatherer;

public class ConfigReceiver {
    private final EmotionDataGatherer emotionDataGatherer;
    private final Context context;
    private final CommunicationConfig config;
    private MqttAndroidClient client;

    public ConfigReceiver(Context context, CommunicationConfig config, EmotionDataGatherer emotionDataGatherer) {
        this.context = context;
        this.config = config;
        this.emotionDataGatherer = emotionDataGatherer;
        try {
            initialize();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void initialize() throws MqttException {
        // start MQTT client with adequate callbacks
        String clientId = MqttClient.generateClientId();
        String serverURI = String.format("%s://%s:%s", config.BROKER_PROTOCOL, config.BROKER_IP_OR_NAME, config.BROKER_PORT);
        client = new MqttAndroidClient(context, serverURI, clientId, new MemoryPersistence(), MqttAndroidClient.Ack.AUTO_ACK);
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.v(this.getClass().getCanonicalName(), "connection lost :(");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                System.out.println("Message arrived! Topic: "+topic+", message:"+new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
        client.connect(null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                try {
                    client.subscribe(config.CONFIGURATION_TOPIC, 0, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.w("Mqtt","Subscribed!");
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.w("Mqtt", "Subscribed fail!");
                        }
                    });

                } catch (MqttException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.w(this.getClass().getCanonicalName(), "Failed to connect");
            }
        });
    }
}
