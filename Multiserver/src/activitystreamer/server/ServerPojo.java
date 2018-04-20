package activitystreamer.server;

import activitystreamer.client.ClientPojo;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerPojo {
    Socket socket;
    String secret;
    List<ClientPojo> clientPojoList;
    ServerPojo connectedServer = null;

    public ServerPojo() {
    }

    public ServerPojo(Socket socket, String secret) {
        this.socket = socket;
        this.secret = secret;
        this.clientPojoList = new ArrayList<>();
    }

    public void addClients(ClientPojo client){
        this.clientPojoList.add(client);
    }
    public void addServer(ServerPojo connectedServer){
        this.connectedServer = connectedServer;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public List<ClientPojo> getClientPojoList() {
        return clientPojoList;
    }

    public void setClientPojoList(List<ClientPojo> clientPojoList) {
        this.clientPojoList = clientPojoList;
    }

    public ServerPojo getConnectedServer() {
        return connectedServer;
    }

    public void setConnectedServer(ServerPojo connectedServer) {
        this.connectedServer = connectedServer;
    }
}
