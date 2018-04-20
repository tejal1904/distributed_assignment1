package activitystreamer.client;

import java.net.Socket;

public class ClientPojo {
    String username;
    String secret;
    Socket socket;

    public ClientPojo() {
    }

    public ClientPojo(String username, String secret, Socket socket) {
        this.username = username;
        this.secret = secret;
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    @Override
    public String toString() {
        return "ClientPojo{" +
                "username='" + username + '\'' +
                ", secret='" + secret + '\'' +
                ", socket=" + socket +
                '}';
    }
}
