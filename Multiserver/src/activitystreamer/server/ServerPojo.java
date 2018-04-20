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

    protected static ServerPojo serverPojo = null;

    public static ServerPojo getInstance() {
        if(serverPojo==null){
            serverPojo=new ServerPojo();
            serverPojo.clientPojoList = new ArrayList<ClientPojo>();
        }
        return serverPojo;
    }

    public void addClients(ClientPojo client){
        clientPojoList.add(client);
    }
    public void addServer(ServerPojo connectedServer){
        connectedServer = connectedServer;
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
        secret = secret;
    }

    public List<ClientPojo> getClientPojoList() {
        return clientPojoList;
    }

    public void setClientPojoList(List<ClientPojo> clientPojoList) {
        clientPojoList = clientPojoList;
    }

    public ServerPojo getConnectedServer() {
        return connectedServer;
    }

    public void setConnectedServer(ServerPojo connectedServer) {
        connectedServer = connectedServer;
    }

    @Override
    public String toString() {
        return "ServerPojo{" +
                "socket=" + socket +
                ", secret='" + secret + '\'' +
                ", clientPojoList=" + clientPojoList +
                ", connectedServer=" + connectedServer +
                '}';
    }
}
