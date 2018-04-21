package activitystreamer.server;

import activitystreamer.client.ClientPojo;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerPojo {

    String secret;
    List<ClientPojo> clientPojoList;
    ServerPojo parentServer = null;
    List<ServerPojo> childServerList;
    int port;
    String hostName;

    protected static ServerPojo serverPojo = null;

    /*public ServerPojo() {
    }*/

    public static ServerPojo getInstance() {
        if(serverPojo == null){
            serverPojo = new ServerPojo();
            serverPojo.childServerList = new ArrayList<ServerPojo>();
            serverPojo.clientPojoList = new ArrayList<ClientPojo>();
        }
        return serverPojo;
    }

    public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

    public void addClients(ClientPojo client){
        clientPojoList.add(client);
    }

    public void addChildServers(ServerPojo serverPojo) { childServerList.add(serverPojo);}

    public void addParentServer(ServerPojo connectedServer){
        this.parentServer = connectedServer;
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

    public ServerPojo getParentServer() {
        return parentServer;
    }

    public List<ServerPojo> getChildServerList() {
        return childServerList;
    }


}
