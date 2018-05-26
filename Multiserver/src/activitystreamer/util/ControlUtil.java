package activitystreamer.util;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;

public class ControlUtil {

	public static final String REGISTER = "REGISTER";
	public static final String AUTHENTICATION = "AUTHENTICATE";
	public static final String LOGIN = "LOGIN";
	public static final String ACTIVITY_MESSAGE = "ACTIVITY_MESSAGE";
	public static final String SERVER_ANNOUNCE = "SERVER_ANNOUNCE";
	public static final String LOGOUT = "LOGOUT";
	public static final String ACTIVITY_BROADCAST = "ACTIVITY_BROADCAST";
	public static final String LOCK_REQUEST = "LOCK_REQUEST";
	public static final String LOCK_DENIED = "LOCK_DENIED";
	public static final String LOCK_ALLOWED = "LOCK_ALLOWED";
	private static final String SERVER = "SERVER";
	private static final String SERVER_BROKEN = "SERVER_BROKEN";
	private static final String AUTHENTICATE_SUCCESS = "AUTHENTICATE_SUCCESS";
	private static final String SERVER_JOIN = "SERVER_JOIN";
	private static final String MESSAGE_STATUS = "MESSAGE_STATUS";
	private static final String SEND_ACKNOWLEDGMENT = "ACKNOWLEDGMENT";
	public static Map<String, Integer> lockAllowedCount = new HashMap<>();
	public Map<String, JSONObject> serverList = new ConcurrentHashMap<String, JSONObject>();
	MapComparator mapComparator = new MapComparator(serverList);
	public Map<String, JSONObject> sortedServerList = new ConcurrentSkipListMap<>(mapComparator);
	public Map<Connection, Queue<JSONObject>> localMessageQueueList = new ConcurrentHashMap<Connection, Queue<JSONObject>>();
	public Map<String, Map<Connection, Queue<JSONObject>>> globalMessageQueueList = new ConcurrentHashMap<String, Map<Connection, Queue<JSONObject>>>();
	JSONObject resultOutput;
	JSONParser parser = new JSONParser();
	Control controlInstance = Control.getInstance();

	protected static ControlUtil controlUtil = null;

	public static ControlUtil getInstance() {
		if (controlUtil == null) {
			controlUtil = new ControlUtil();
		}
		return controlUtil;
	}

	@SuppressWarnings("unchecked")
	public boolean processCommands(Connection connection, String message) {
		JSONObject msg;
		resultOutput = new JSONObject();
		try {
			msg = (JSONObject) parser.parse(message);
			String command = (String) msg.get("command");
			if (command == null) {
				resultOutput.put("command", "INVALID_MESSAGE");
				resultOutput.put("info", "The received message did not contain a command");
				connection.writeMsg(resultOutput.toJSONString());
				return true;
			}
			switch (command) {
			case ControlUtil.REGISTER:
				return registerClient(msg,connection);
			case ControlUtil.AUTHENTICATION:
				return authenticateServer(msg, connection);
			case ControlUtil.SERVER_JOIN:
				return handleServerJoin(msg,connection);
			case ControlUtil.LOGIN:
				return loginUtil(connection, msg);
			case ControlUtil.LOGOUT:
				connection.setLoggedInClient(false);
				return true;				
			case ControlUtil.ACTIVITY_MESSAGE:
				return activityMessageUtil(connection, msg);
			case ControlUtil.ACTIVITY_BROADCAST:
				return activityBroadcastUtil(connection, msg);
			case ControlUtil.SERVER_ANNOUNCE:
				return serverAnnounce(msg, connection);			
			case ControlUtil.SERVER_BROKEN:
				return broadcastServerBroken(msg, connection);
			case ControlUtil.AUTHENTICATE_SUCCESS:
				return handleAuthSuccess(msg,connection);
            case ControlUtil.MESSAGE_STATUS:
                return updateGlobalMessages(msg, connection);
			case ControlUtil.SEND_ACKNOWLEDGMENT:
				return handleGetAcknowledgment(msg,connection);
			default:
				resultOutput.put("command", "INVALID_MESSAGE");
				resultOutput.put("info", "Invalid command");
				connection.writeMsg(resultOutput.toJSONString());
				return true;
			}
		} catch (ParseException e) {
			resultOutput.put("command", "INVALID_MESSAGE");
			resultOutput.put("info", "JSON parse error while parsing message");
			try {
				connection.writeMsg(resultOutput.toJSONString());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;

	}

    @SuppressWarnings("unchecked")
	private boolean updateGlobalMessages(JSONObject msg, Connection connection) throws IOException {
        globalMessageQueueList.put(connection.getConnectedServerId(),  (Map<Connection,Queue<JSONObject>>) msg.get("queue"));
        return false;
    }

    @SuppressWarnings("unchecked")
	private boolean authenticateServer(JSONObject msg, Connection connection) throws IOException {
		String secret1 = (String) msg.get("secret");
		String info = processAuthenticate(connection, msg);
		if (info.equals("SUCCESS")) {
			//code for message starts
			/*
			* check if msg had failureServer command that means its failure case
			* then check globalMessageList for toConnectedServer connection object and if present then
			* take that messagequeue from globalMessageList and add local queue in it and replace local queue with this
			* else
			* create a new messagePojo with new queue
			*
			* */
			boolean sendFailureServer = false;
			if(msg.get("failureServerId") != null){
				
				Queue<JSONObject> serverQueue = new LinkedList<>();
				Queue<JSONObject> messageQueue = new LinkedList<>();
				String failureServerId = (String) msg.get("failureServerId");
				Iterator<Map.Entry<Connection, Queue<JSONObject>>> iterator = localMessageQueueList.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<Connection, Queue<JSONObject>> entry = iterator.next();
					if(entry.getKey().getConnectedServerId().equals(failureServerId)) {
						serverQueue = entry.getValue();
					}				
				}
				Map<Connection, Queue<JSONObject>> globalListofFailedServer = globalMessageQueueList.get(failureServerId);
				Iterator<Map.Entry<Connection, Queue<JSONObject>>> iterator1 = localMessageQueueList.entrySet().iterator();
				while (iterator1.hasNext()) {
					Map.Entry<Connection, Queue<JSONObject>> entry = iterator1.next();
					if(entry.getKey().getConnectedServerId().equals((String) msg.get("id"))) {
						messageQueue = entry.getValue();
						messageQueue.addAll(serverQueue);
					}				
				}
				localMessageQueueList.put(connection, messageQueue);
			}else{
				Queue queue = new LinkedList<>();
				localMessageQueueList.put(connection, queue);
			}
			//code for messaging ends

			int templevel = controlInstance.getLevel()+1;
			int temprank = 0;
			if(controlInstance.getLevelRank().containsKey(templevel)){
				temprank = controlInstance.getLevelRank().get(templevel) + 1;
			}
			controlInstance.getLevelRank().put(templevel,  temprank);
			System.out.println("in authenticate success for: "+msg.get("id"));
			System.out.println("giving level:"+templevel + " rank: "+temprank);
			connection.setName(ControlUtil.SERVER);
			connection.setConnectedServerId((String) msg.get("id"));
			resultOutput.put("command", "AUTHENTICATE_SUCCESS");
			resultOutput.put("serverDetail", Settings.getId());
			resultOutput.put("clientList",Control.getInstance().getGlobalRegisteredClients());
			resultOutput.put("level", templevel);
			resultOutput.put("rank", temprank);
			if(sendFailureServer) {
				resultOutput.put("failureServerId", msg.get("failureServerId"));
			}
			connection.writeMsg(resultOutput.toJSONString());
			//sending Server join broadcast message
			for (Connection conn : controlInstance.getConnections()) {
				if (conn.isOpen() && conn.getName().equals(ControlUtil.SERVER)) {
					try {
						JSONObject output = new JSONObject();
						output.put("command", "SERVER_JOIN");							
						output.put("level",templevel);
						output.put("rank", temprank);						
						conn.writeMsg(output.toJSONString());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return false;
		} else if (info.equals("AUTHENTICATION_FAIL")) {
			resultOutput.put("command", info);
			resultOutput.put("info", "the supplied secret is incorrect: " + secret1);
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private boolean handleServerJoin(JSONObject msg, Connection connection) throws IOException {		
		if(!controlInstance.getLevelRank().containsKey(((Long) msg.get("level")).intValue())){
			controlInstance.getLevelRank().put(((Long) msg.get("level")).intValue(), ((Long) msg.get("rank")).intValue());
		}else if(controlInstance.getLevelRank().get(((Long) msg.get("level")).intValue()) < ((Long) msg.get("rank")).intValue()){
			controlInstance.getLevelRank().replace(((Long) msg.get("level")).intValue(),((Long) msg.get("rank")).intValue());
		}
		System.out.println("printing levelRank map: "+controlInstance.getLevelRank());	
			
		return false;
	}



	@SuppressWarnings("unchecked")
	private boolean registerClient(JSONObject msg, Connection connection) throws IOException {
		String username = (String) msg.get("username");
		String secret = (String) msg.get("secret");
		
		if (!controlInstance.getRegisteredClients().containsKey(username) && !controlInstance.getGlobalRegisteredClients().containsKey(username)) {
			resultOutput.put("command", "REGISTER_SUCCESS");
			resultOutput.put("info", "register success for " + username);
			connection.writeMsg(resultOutput.toJSONString());
			controlInstance.addRegisteredClients(username, secret);
			return false;
		} else if(connection.isLoggedInClient() == true) {
			resultOutput.put("command", "INVALID_MESSAGE");
			resultOutput.put("info", "Client already logged in to the system");
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		}else {	
			resultOutput.put("command", "REGISTER_FAILED");
			resultOutput.put("info", username + " is already registered with the system");
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		}		
	}	

	private String processAuthenticate(Connection connection, JSONObject msg) {
		System.out.println("in authenticate: "+connection.getSocket().getInetAddress());
		String secret = (String) msg.get("secret");

		if (Settings.getSecret().equals(secret)) {
			return "SUCCESS";
		} else {
			return "AUTHENTICATION_FAIL";
		}
	}

	private boolean serverAnnounce(JSONObject msg, Connection connection) throws IOException {
		//System.out.println(msg.get("clientList"));
		if (null != msg.get("id")) {
			serverList.put((String) msg.get("id"), msg);
			Map<String,String> receivedClients = (Map<String,String>) msg.get("clientList");
			Iterator clientIterator = receivedClients.entrySet().iterator();
			while (clientIterator.hasNext()) {
				Map.Entry client = (Map.Entry)clientIterator.next();
				String clientUsername = (String) client.getKey();
				String clientPassword = (String) client.getValue();
				if (!controlInstance.getGlobalRegisteredClients().containsKey(clientUsername)) {
					controlInstance.addGlobalRegisteredClients(clientUsername,clientPassword);
				}
			}
			broadcastUtil(connection, msg);			
		}
		//System.out.println("in server announce: "+serverList);
		return false;
	}

	private boolean activityBroadcastUtil(Connection connection, JSONObject msg) {
		try {
			JSONObject sendAcknowledgment = new JSONObject();
			sendAcknowledgment.put("command", "ACKNOWLEDGMENT");
			sendAcknowledgment.put("fromServer",Settings.getId());
			connection.writeMsg(sendAcknowledgment.toJSONString());

			JSONObject activity = (JSONObject) msg.get("activity");

			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while (listIterator.hasNext()) {
				Connection connection1 = listIterator.next();
				boolean isSameConnection = (connection1.getSocket().getInetAddress() == connection.getSocket()
						.getInetAddress());

				if (!isSameConnection
						&& ((!connection1.getName().equals(ControlUtil.SERVER) && connection1.isLoggedInClient()))) {
						connection1.writeMsg(msg.toJSONString());
				} else if(!isSameConnection && connection1.getName().equals(ControlUtil.SERVER)) {
					Queue<JSONObject> localqueue = localMessageQueueList.get(connection1);
					if(localqueue.isEmpty()) {
						JSONObject sendbroadcast = new JSONObject();
						sendbroadcast.put("command", "ACTIVITY_BROADCAST");
						sendbroadcast.put("activity", activity);
						connection1.writeMsg(sendbroadcast.toJSONString());
					}
                    activity.put("count", 1);
                    localqueue.add(activity);
                    localMessageQueueList.put(connection1, localqueue);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean activityMessageUtil(Connection connection, JSONObject msg) throws IOException {
		String username = (String) msg.get("username");
		String secret = (String) msg.get("secret");
		if (username.equals("anonymous")) {
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while (listIterator.hasNext()) {
				Connection connection1 = listIterator.next();

				if (connection1.isLoggedInClient()) {
					resultOutput.put("command", "ACTIVITY_BROADCAST");
					resultOutput.put("activity", msg.get("activity"));
					connection1.writeMsg(resultOutput.toJSONString());
				}
			}
		}

		if ((!connection.isLoggedInClient())) {
			resultOutput.put("command", "AUTHENTICATION_FAIL");
			resultOutput.put("info","must send a login message first");
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		} else if(!(controlInstance.getRegisteredClients().containsKey(username)
				&& controlInstance.getRegisteredClients().get(username).equals(secret))){
			resultOutput.put("command", "AUTHENTICATION_FAIL");
			resultOutput.put("info","username and/or secret is incorrect");
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		} else{
			JSONObject activity = (JSONObject) msg.get("activity");
			activity.put("authenticated_user", username);
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while (listIterator.hasNext()) {
				Connection connection1 = listIterator.next();
				if(!connection1.getName().equals(ControlUtil.SERVER) && connection1.isLoggedInClient()) {
					//if client send the message received
					resultOutput.put("command", "ACTIVITY_BROADCAST");
					resultOutput.put("activity", activity);
					connection1.writeMsg(resultOutput.toJSONString());
				}else {
					Queue<JSONObject> localqueue = localMessageQueueList.get(connection1);
					if(localqueue != null && localqueue.isEmpty()) {
						JSONObject sendbroadcast = new JSONObject();
						sendbroadcast.put("command", "ACTIVITY_BROADCAST");
						sendbroadcast.put("activity", activity);
						connection1.writeMsg(sendbroadcast.toJSONString());
					}
                    activity.put("count", 1);
                    localqueue.add(activity);
                    localMessageQueueList.put(connection1, localqueue);					
				}
			}
			return false;
		}

	}

	@SuppressWarnings("unchecked")
	private boolean loginUtil(Connection connection, JSONObject msg) throws IOException {
		String secret;
		String username1;
		username1 = (String) msg.get("username");
		secret = (String) msg.get("secret");
		String info = checkCredentials(username1, secret);
		if (info.equals("success")) {
			connection.setLoggedInClient(true);
			resultOutput.put("command", "LOGIN_SUCCESS");
			resultOutput.put("info", "logged in as user " + username1);
			connection.writeMsg(resultOutput.toJSONString());
			Iterator<String> stringIterator = serverList.keySet().iterator();
			while (stringIterator.hasNext()) {
				String object = stringIterator.next();
				if (null != object && object != Settings.getId()) {
					if (controlInstance.getLoad() >= ((Long) serverList.get(object).get("load")).intValue() + 2) {
						resultOutput = new JSONObject();
						resultOutput.put("command", "REDIRECT");
						resultOutput.put("hostname", (String) serverList.get(object).get("hostname"));
						resultOutput.put("port", String.valueOf(serverList.get(object).get("port")));
						connection.writeMsg(resultOutput.toJSONString());
						return true;
					}
				}
			}
			return false;
		} else {
			resultOutput.put("command", "LOGIN_FAILED");
			resultOutput.put("info", info);
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		}
	}

	public String checkCredentials(String username, String password) {
		Map<String, String> registeredUsers = controlInstance.getRegisteredClients();
		Map<String, String> globalClients = controlInstance.getGlobalRegisteredClients();
		if ((registeredUsers.containsKey(username) && (registeredUsers.get(username).equals(password))) || (globalClients.containsKey(username) &&
				globalClients.get(username).equals(password))
				|| username.equals("anonymous")) {
			return "success";
		} else if ((registeredUsers.containsKey(username) && !(registeredUsers.get(username).equals(password))) || (globalClients.containsKey
				(username) &&
				!globalClients.get(username).equals(password))) {
			return "attempt to login with wrong secret";
		} else if (!registeredUsers.containsKey(username)) {
			return "client is not registered with the server";
		} else {
			return "";
		}
	}
	
	private void broadcastUtil(Connection connection, JSONObject msg) {
		try {
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while (listIterator.hasNext()) {
				Connection connection1 = listIterator.next();
				boolean isSameConnection = (connection1.getSocket().getInetAddress() == connection.getSocket()
						.getInetAddress());
				if (!isSameConnection && connection1.getName().equals(ControlUtil.SERVER)) {
					connection1.writeMsg(msg.toJSONString());
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void sendConnectionLostMessage(Connection con) {		
		String failedServerId = con.getConnectedServerId();
		if(con.isChild()) {
			//establish new connection to its parent.
			Socket newServer = getSocketDetails(failedServerId);
			if(newServer != null) {
				try {
					System.out.println("...connection success...going for outgoingconnection...");
					controlInstance.outgoingConnectionForReconnect(newServer, failedServerId);
					System.out.println("outgoing connection success");
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}
		} else {
			//When it is a parent and one of it's child has failed
			//check if it is the only connected server (ie the crashed server is the leaf node),
			//then get its messages and add it to all the connection's queues if not empty, else add and broadcast
			Map<Connection, Queue<JSONObject>> failedGlobalQueueList = globalMessageQueueList.get(failedServerId);
			Queue<JSONObject> messageQueue = new LinkedList<>();
			if(failedGlobalQueueList != null && failedGlobalQueueList.size() == 1) { //To make sure it does not have any other child
				Iterator<Map.Entry<Connection, Queue<JSONObject>>> iterator = failedGlobalQueueList.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<Connection, Queue<JSONObject>> entry = iterator.next();
					if(entry.getKey().getConnectedServerId().equals(Settings.getId())) {
						messageQueue = entry.getValue();
					}				
				}
				
			}
			
			while(messageQueue!= null && !messageQueue.isEmpty()) {
				JSONObject msg = messageQueue.poll();
				msg.remove("count");
				
				ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
				while (listIterator.hasNext()) {
					Connection connection1 = listIterator.next();
					if(!connection1.getName().equals(ControlUtil.SERVER) && connection1.isLoggedInClient()) {
						//if client send the message received
						resultOutput.put("command", "ACTIVITY_BROADCAST");
						resultOutput.put("activity", msg);
						try {
							connection1.writeMsg(resultOutput.toJSONString());
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						Queue<JSONObject> localqueue = localMessageQueueList.get(connection1);
						if(localqueue.isEmpty()) {
							JSONObject sendbroadcast = new JSONObject();
							sendbroadcast.put("command", "ACTIVITY_BROADCAST");
							sendbroadcast.put("activity", msg);
							try {
								connection1.writeMsg(sendbroadcast.toJSONString());
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
	                    msg.put("count", 1);
	                    localqueue.add(msg);
	                    localMessageQueueList.put(connection1, localqueue);
						
					}
				}
			}
		}
		for (Connection connection : controlInstance.getConnections()) {
			if (connection.isOpen() && connection.getName().equals(ControlUtil.SERVER)) {
				try {
					JSONObject output = new JSONObject();
					output.put("command", "SERVER_BROKEN");
					output.put("serverId", con.getConnectedServerId());
					connection.writeMsg(output.toJSONString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}
	
	private boolean broadcastServerBroken(JSONObject msg, Connection connection) {
		if(serverList.containsKey((String) msg.get("serverId"))) {
			serverList.remove(msg.get("serverId"));
		}
		
		broadcastUtil(connection, msg);
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private boolean handleAuthSuccess(JSONObject msg, Connection connection) {
		//code for messaging starts
		/*
		* check if msg has failureServerId
		* if yes
		* fetch toConnectedServer.getServerid == failuerServerId then get queue from localMessageList
		* and
		* else
		* create new MessagePojo object and add in localMessageList
		* */
		if(msg.get("failureServerId") != null){
			Queue<JSONObject> serverQueue = new LinkedList<>();
			String failureServerId = (String) msg.get("failureServerId");
			Iterator<Map.Entry<Connection, Queue<JSONObject>>> iterator = localMessageQueueList.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Connection, Queue<JSONObject>> entry = iterator.next();
				if(entry.getKey().getConnectedServerId().equals(failureServerId)) {
					serverQueue = entry.getValue();
				}				
			}
			
			localMessageQueueList.put(connection, serverQueue);
		}else{
			Queue<JSONObject> queue = new LinkedList<>();
			localMessageQueueList.put(connection, queue);
		}
		//code for messaging ends
		
		
		Map<String,String> receivedClients = (Map<String,String>) msg.get("clientList");
		Iterator clientIterator = receivedClients.entrySet().iterator();
		while (clientIterator.hasNext()) {
			Map.Entry client = (Map.Entry)clientIterator.next();
			String clientUsername = (String) client.getKey();
			String clientPassword = (String) client.getValue();
			if (!controlInstance.getGlobalRegisteredClients().containsKey(clientUsername)) {
				controlInstance.addGlobalRegisteredClients(clientUsername,clientPassword);
			}
		}		
		connection.setChild(true);
		connection.setConnectedServerId((String) msg.get("serverDetail"));
		controlInstance.setParentServerId((String) msg.get("serverDetail"));
		controlInstance.setLevel(((Long) msg.get("level")).intValue());
		controlInstance.setRank(((Long) msg.get("rank")).intValue());	
		
		JSONObject ownDetails = new JSONObject();
		ownDetails.put("hostname", Settings.getLocalHostname());
		ownDetails.put("port", Settings.getLocalPort());
		ownDetails.put("id", Settings.getId());
		ownDetails.put("clientList", controlInstance.getRegisteredClients());
		ownDetails.put("parentServerName",Settings.getRemoteHostname());
		ownDetails.put("parentServerPort", Settings.getRemotePort());
		ownDetails.put("parentId", controlInstance.getParentServerId());
		ownDetails.put("level", Long.valueOf(controlInstance.getLevel()));
		ownDetails.put("rank", Long.valueOf(controlInstance.getRank()));
		serverList.put(Settings.getId(), ownDetails);
		return false;
	}
	
	private Socket getSocketDetails(String serverId) {
		JSONObject serverDetails = serverList.get(serverId);
		Socket newSocket = null;
		serverList.remove(serverId);
		String parentId = (serverDetails != null && (serverDetails.get("parentId") != null)) ? (String) serverDetails.get("parentId") : null;
		System.out.println("My server list: "+ serverList);
		if(parentId != null) {
			try {
				System.out.println("Trying to connect to ....."+ (String) serverDetails.get("parentServerName"));
				System.out.println("SERVER LIST*************:"+ serverList);
				newSocket = new Socket((String) serverDetails.get("parentServerName"), ((Long) serverDetails.get("parentServerPort")).intValue());
				Settings.setRemoteHostname((String) serverDetails.get("parentServerName"));
				Settings.setRemotePort(((Long) serverDetails.get("parentServerPort")).intValue());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				newSocket = getSocketDetails(parentId);
			} 
		} else {
			System.out.println("in else i.e parent id is null");
			sortedServerList.clear();
			sortedServerList.putAll(serverList);
			System.out.println("sorted map: "+sortedServerList);
			Iterator<Map.Entry<String, JSONObject>> iterator = sortedServerList.entrySet().iterator();
			while (iterator.hasNext()){
				Map.Entry<String, JSONObject> entry = iterator.next();
				JSONObject json = entry.getValue();
				String key = entry.getKey();
				if(key.equals(Settings.getId())){
					System.out.println("my entry in map");
					
					//Update self details
					Settings.setRemoteHostname(null);
					Settings.setRemotePort(0);
					controlInstance.setParentServerId(null);
					
					//Update self details in the server list
					JSONObject selfDetails = serverList.get(Settings.getId());
					selfDetails.put("parentServerName",Settings.getRemoteHostname());
					selfDetails.put("parentServerPort", Settings.getRemotePort());
					selfDetails.put("parentId", controlInstance.getParentServerId());
					serverList.put(Settings.getId(), selfDetails);
					
					//if entry in map is same as self then do nothing and break
					break;
				}else{
					//else try connecting with server in list in order
					try {
						System.out.println("trying to connect to "+entry.getValue().get("hostname") + "  port: " + entry.getValue().get("port"));
						newSocket = new Socket((String) entry.getValue().get("hostname"), ((Long) entry.getValue().get("port")).intValue());
						Settings.setRemoteHostname((String) entry.getValue().get("hostname"));
						Settings.setRemotePort(((Long) entry.getValue().get("port")).intValue());
						break;
					} catch (IOException e) {
						//in case of exception just continue and connect to next server
						continue;
					}
				}
			}

		}
		return newSocket;
	}

	@SuppressWarnings("unchecked")
	private boolean handleGetAcknowledgment(JSONObject msg, Connection connection) {
		//put timer logic to return acknowledgment
		Queue<JSONObject> messageQueue = localMessageQueueList.get(connection);
		messageQueue.remove();
		if(!messageQueue.isEmpty()) {
			JSONObject activityMessage = messageQueue.peek();
			JSONObject sendQueueMessage = new JSONObject();
			sendQueueMessage.put("command", "ACTIVITY_BROADCAST");
			sendQueueMessage.put("activity", activityMessage);
			try {
				connection.writeMsg(sendQueueMessage.toJSONString());
			} catch (IOException e) {
				e.printStackTrace();
			}
			messageQueue.peek().put("count", 1);
		}	
		
		return false;
	}

}
