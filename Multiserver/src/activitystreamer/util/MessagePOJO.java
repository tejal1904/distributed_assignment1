package activitystreamer.util;

import activitystreamer.server.Connection;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.Queue;

@JsonSerialize
public class MessagePOJO {
    private Queue<JSONObject> messageQueue = new LinkedList<>();
    private Connection toConnection;
    private String fromServerId;
    private int count = 0;

    public Queue<JSONObject> getMessageQueue() {
        return messageQueue;
    }

    public void setMessageQueue(Queue<JSONObject> messageQueue) {
        this.messageQueue = messageQueue;
    }

    public Connection getToConnection() {
        return toConnection;
    }

    public void setToConnection(Connection toConnection) {
        this.toConnection = toConnection;
    }

    public String getFromServerId() {
        return fromServerId;
    }

    public void setFromServerId(String fromServerId) {
        this.fromServerId = fromServerId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessagePOJO that = (MessagePOJO) o;

        if (!messageQueue.equals(that.messageQueue)) return false;
        if (!toConnection.equals(that.toConnection)) return false;
        return fromServerId.equals(that.fromServerId);
    }

    @Override
    public int hashCode() {
        int result = messageQueue.hashCode();
        result = 31 * result + toConnection.hashCode();
        result = 31 * result + fromServerId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MessagePOJO{" +
                "messageQueue=" + messageQueue +
                ", toConnection=" + toConnection +
                ", fromServerId='" + fromServerId + '\'' +
                '}';
    }
}
