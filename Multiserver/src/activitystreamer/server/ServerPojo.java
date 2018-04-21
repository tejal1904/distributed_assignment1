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
        serverPojo.connectedServer = connectedServer;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        serverPojo.socket = socket;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        serverPojo.secret = secret;
    }

    public List<ClientPojo> getClientPojoList() {
        return clientPojoList;
    }

    public ServerPojo getConnectedServer() {
        return connectedServer;
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
