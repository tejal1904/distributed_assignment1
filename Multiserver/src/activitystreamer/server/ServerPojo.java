package activitystreamer.server;

import activitystreamer.client.ClientPojo;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerPojo {
    ServerSocket socket;
    String secret;
    List<ClientPojo> clientPojoList;
    ServerPojo parentServer = null;
    List<ServerPojo> childServerList;

    protected static ServerPojo serverPojo = null;

    public static ServerPojo getInstance() {
        if(serverPojo==null){
            serverPojo=new ServerPojo();
            serverPojo.clientPojoList = new ArrayList<ClientPojo>();
            serverPojo.childServerList = new ArrayList<ServerPojo>();
        }
        return serverPojo;
    }

    public void addClients(ClientPojo client){
        clientPojoList.add(client);
    }

    public void addChildServers(ServerPojo serverPojo) { childServerList.add(serverPojo);}

    public void addParentServer(ServerPojo connectedServer){
        serverPojo.parentServer = connectedServer;
    }

    public ServerSocket getSocket() {
        return socket;
    }

    public void setSocket(ServerSocket socket) {
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

    public ServerPojo getParentServer() {
        return parentServer;
    }

    public List<ServerPojo> getChildServerList() {
        return childServerList;
    }

}
