package activitystreamer.util;

import activitystreamer.server.Connection;
import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.Queue;

public class MessagePOJO {
    private Queue<JSONObject> q;
    Connection toConnection;
    String fromServerId;

    public Queue<JSONObject> getQ() {
        return q;
    }

    public void setQ(Queue<JSONObject> q) {
        this.q = q;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessagePOJO that = (MessagePOJO) o;

        if (!q.equals(that.q)) return false;
        if (!toConnection.equals(that.toConnection)) return false;
        return fromServerId.equals(that.fromServerId);
    }

    @Override
    public int hashCode() {
        int result = q.hashCode();
        result = 31 * result + toConnection.hashCode();
        result = 31 * result + fromServerId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MessagePOJO{" +
                "q=" + q +
                ", toConnection=" + toConnection +
                ", fromServerId='" + fromServerId + '\'' +
                '}';
    }
}
