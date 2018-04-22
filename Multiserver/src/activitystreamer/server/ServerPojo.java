package activitystreamer.server;

import activitystreamer.client.ClientPojo;
import com.sun.xml.internal.bind.v2.TODO;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerPojo {

    String secret;
    int port;
    int load;
    String hostName;
    ServerPojo parentServer = null;
    List<ClientPojo> clientPojoList;
    List<ServerPojo> childServerList;
    Map<String,ServerPojo> serverLoadMap = null;
//    TODO: check tree structure

    protected static ServerPojo serverPojo = null;

    public static ServerPojo getInstance() {
        if(serverPojo == null){
            serverPojo = new ServerPojo();
            serverPojo.childServerList = new ArrayList<ServerPojo>();
            serverPojo.clientPojoList = new ArrayList<ClientPojo>();
            serverPojo.serverLoadMap = new HashMap<String, ServerPojo>();
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

    public void addServerLoad(String key,ServerPojo pojo){
        serverLoadMap.put(key, pojo);
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getLoad() {
        return load;
    }

    public void setLoad(int load) {
        this.load = load;
    }

    public Map<String, ServerPojo> getServerLoad() {
        return serverLoadMap;
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
