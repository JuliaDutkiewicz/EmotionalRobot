package pl.edu.agh.emotionalrobot.communication;

import org.json.JSONException;
import org.json.JSONObject;

public class CommunicationConfig {
    public final String BROKER_IP_OR_NAME;
    public final String BROKER_PORT;
    public final String BROKER_PROTOCOL;
    public final String UPDATE_TOPIC;
    public final String CONFIGURATION_TOPIC;
    public final int STARTING_UPDATE_INTERVAL;

    public CommunicationConfig(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        BROKER_IP_OR_NAME = jsonObject.getString("BROKER_IP_OR_NAME");
        BROKER_PORT = jsonObject.getString("BROKER_PORT");
        BROKER_PROTOCOL = jsonObject.getString("BROKER_PROTOCOL");
        UPDATE_TOPIC = jsonObject.getString("BASE_TOPIC") + jsonObject.getString("UPDATE_TOPIC_SUFFIX");
        CONFIGURATION_TOPIC = jsonObject.getString("BASE_TOPIC") + jsonObject.getString("CONFIGURATION_TOPIC_SUFFIX");
        STARTING_UPDATE_INTERVAL = Integer.parseInt(jsonObject.getString("DEFAULT_TICK_LENGTH"));
    }
}
