package pl.edu.agh.emotionalrobot.communication;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;

import pl.edu.agh.emotionalrobot.EmotionDataGatherer;

public class ConfigReceiver {
    private final EmotionDataGatherer emotionDataGatherer;
    private final Context context;
    private final CommunicationConfig config;
    private static final String UPDATE_CYCLE_ON = "UPDATE_CYCLE_ON";
    private static final String TICK_LENGTH = "TICK_LENGTH";
    private static final String UPDATE_TYPE = "UPDATE_TYPE";
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
                emotionDataGatherer.updateSender.sendUpdate(new Date(),new HashMap<String, Float>(),"lost","audio");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Toast.makeText(context, "Message arrived! Topic: " + topic + ", message:" + new String(message.getPayload()), Toast.LENGTH_LONG).show();
                emotionDataGatherer.updateSender.sendUpdate(new Date(),new HashMap<String, Float>(),"cos","audio");
                Log.w(this.getClass().getCanonicalName(), "Message arrived! Topic: " + topic + ", message:" + new String(message.getPayload()));
                JSONObject config = new JSONObject(new String(message.getPayload()));
                try {
                    setConfig(config);
                }catch(Exception e) {
                    Log.v(this.getClass().getCanonicalName(), "Exception occurred when setting configuration", e);
                }
                Toast.makeText(context, "Message arrived moment ago! Topic: " + topic + ", message:" + new String(message.getPayload()), Toast.LENGTH_LONG).show();
            }

            private void setConfig(JSONObject config) throws JSONException {
                if(config.has(TICK_LENGTH)) {
                    emotionDataGatherer.stopSendingUpdates();
                    emotionDataGatherer.setUpdateInterval(config.getInt(TICK_LENGTH));
                    emotionDataGatherer.startSendingUpdates();
                }
                if(config.has(UPDATE_TYPE)){
                    UpdateType updateType = config.getString(UPDATE_TYPE).length() == 1
                            ? UpdateType.values()[config.getInt(UPDATE_TYPE)]
                            : UpdateType.valueOf(config.getString(UPDATE_TYPE));
                    emotionDataGatherer.setUpdateType(updateType);
                }
                if (config.has(UPDATE_CYCLE_ON)) {
                    if (Boolean.parseBoolean(config.getString(UPDATE_CYCLE_ON))) {
                        emotionDataGatherer.startSendingUpdates();
                    } else {
                        emotionDataGatherer.stopSendingUpdates();
                    }
                }
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
                            Log.w("Mqtt", "Subscribed! topic: "+config.CONFIGURATION_TOPIC);
                            emotionDataGatherer.updateSender.sendUpdate(new Date(),new HashMap<String, Float>(),"subscribed!","audio");
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            emotionDataGatherer.updateSender.sendUpdate(new Date(),new HashMap<String, Float>(),"subscribed fail","audio");
                            Log.d("Mqtt - config", "subscribed fail", exception);
                            Toast.makeText(context, "Mqtt: " + "Subscribed fail!", Toast.LENGTH_LONG).show();
                        }
                    });
                    Log.d("config",config.CONFIGURATION_TOPIC);

                } catch (MqttException ex) {
                    ex.printStackTrace();
                    emotionDataGatherer.updateSender.sendUpdate(new Date(),new HashMap<String, Float>(),ex.getMessage(),"audio");
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.w(this.getClass().getCanonicalName(), "Failed to connect");
                emotionDataGatherer.updateSender.sendUpdate(new Date(),new HashMap<String, Float>(),"failed to connect","audio");
            }
        });
    }
}
