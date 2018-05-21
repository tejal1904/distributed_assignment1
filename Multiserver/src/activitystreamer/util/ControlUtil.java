package activitystreamer.util;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;

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
	private static final String GLOBAL_CLIENTS = "GLOBAL_CLIENTS";
	private static final String AUTHENTICATE_SUCCESS = "AUTHENTICATE_SUCCESS";
	public static Map<String, Integer> lockAllowedCount = new HashMap<>();
	public static Map<String, JSONObject> serverList = new HashMap<>();
	public static Map<String,List<Connection>> serverClientList = new HashMap<>();

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
			case ControlUtil.LOCK_REQUEST:
				return receiveLockRequestClient(msg, connection);
			case ControlUtil.LOCK_ALLOWED:
				return receiveLockAllowed(msg, connection);
			case ControlUtil.LOCK_DENIED:
				return receiveLockDenied(msg, connection);
			case ControlUtil.SERVER_BROKEN:
				return broadcastServerBroken(msg, connection);
			case ControlUtil.AUTHENTICATE_SUCCESS:
				return handleAuthSuccess(msg,connection);	
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
	private boolean authenticateServer(JSONObject msg, Connection connection) throws IOException {
		String secret1 = (String) msg.get("secret");
		String info = processAuthenticate(connection, msg);
		if (info.equals("SUCCESS")) {
			connection.setName(ControlUtil.SERVER);
			connection.setConnectedServerId((String) msg.get("id")); 
			resultOutput.put("command", "AUTHENTICATE_SUCCESS");
			resultOutput.put("serverDetail", Settings.getId());
			resultOutput.put("clientList",Control.getInstance().getGlobalRegisteredClients());
			connection.writeMsg(resultOutput.toJSONString());
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
	private boolean receiveLockDenied(JSONObject msg, Connection connection) throws IOException {
		if(!connection.getName().equals(ControlUtil.SERVER)){
			resultOutput.put("command", "INVALID_MESSAGE");
			resultOutput.put("info", "received LOCK_DENIED from an unauthenticated server");
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		}
		String username2 = (String) msg.get("username");
		JSONObject object = null;
		Connection connection1 = null;
		if(Control.getInstance().getGlobalRegisteredClients().containsKey(username2)){
			Control.getInstance().getGlobalRegisteredClients().remove(username2);
		}
		for (Map.Entry<JSONObject, Connection> entry : controlInstance.getToBeRegisteredClients().entrySet()) {
			if (username2.equals(entry.getKey().get("username").toString())) {
				object = entry.getKey();
				connection1 = entry.getValue();
			}
		}
		if (null != connection1 && null != object) {		
			controlInstance.getToBeRegisteredClients().remove(object);
			lockAllowedCount.remove(username2);
			resultOutput.put("command", "REGISTER_FAILED");
			resultOutput.put("info",
					object.get("username").toString() + " is already registered with the system");
			connection1.writeMsg(resultOutput.toJSONString());
		}
		
		broadcastUtil(connection, msg);
		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean receiveLockAllowed(JSONObject msg, Connection connection) throws IOException {
		String username1 = (String) msg.get("username");
		if(!connection.getName().equals(ControlUtil.SERVER)){
			resultOutput.put("command", "INVALID_MESSAGE");
			resultOutput.put("info", "received LOCK_ALLOWED from an unauthenticated server");
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		}
		int count = 0;
		if (null != lockAllowedCount.get(username1)) {
			count = lockAllowedCount.get(username1);
		}
		lockAllowedCount.put(username1, count + 1);
		if (serverList.size() == lockAllowedCount.get(username1)) {
			Iterator<JSONObject> iterator = controlInstance.getToBeRegisteredClients().keySet().iterator();
			while (iterator.hasNext()) {
				JSONObject object = iterator.next();
				Connection connection1 = controlInstance.getToBeRegisteredClients().get(object);
				if (null != object && null != connection1) {
					if (username1.equals(object.get("username").toString())) {
						lockAllowedCount.remove(username1);
						controlInstance.addRegisteredClients(username1, object.get("secret").toString());
						resultOutput.put("command", "REGISTER_SUCCESS");
						resultOutput.put("info", "register success for " + username1);
						connection1.writeMsg(resultOutput.toJSONString());
						return false;
					}
				}
			}
		}
		
		broadcastUtil(connection, msg);
		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean receiveLockRequestClient(JSONObject msg, Connection connection) throws IOException {
		// process received lock request
		String username3 = (String) msg.get("username");
		String secret3 = (String) msg.get("secret");
		if(!connection.getName().equals(ControlUtil.SERVER)){
			resultOutput.put("command", "INVALID_MESSAGE");
			resultOutput.put("info", "received LOCK_REQUEST from an unauthenticated server");
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		}

		String data = processLockRequest(msg, connection);
		if (data.equals(LOCK_ALLOWED)) {
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			controlInstance.addGlobalRegisteredClients(username3,secret3);
			while (listIterator.hasNext()) {
				Connection connection1 = listIterator.next();
				if (connection1.getName().equals(ControlUtil.SERVER)) {
					resultOutput.put("command", data);
					resultOutput.put("username", username3);
					resultOutput.put("secret", secret3);
					connection1.writeMsg(resultOutput.toJSONString());
					return false;
				}
			}
		} else if (data.equals(LOCK_DENIED)) {
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while (listIterator.hasNext()) {
				Connection connection1 = listIterator.next();
				if (connection1.getName().equals(ControlUtil.SERVER)) {
					resultOutput.put("command", data);
					resultOutput.put("username", username3);
					resultOutput.put("secret", secret3);
					connection1.writeMsg(resultOutput.toJSONString());
					return false;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private boolean registerClient(JSONObject msg, Connection connection) throws IOException {
		String username = (String) msg.get("username");
		String secret = (String) msg.get("secret");
		
		if (!controlInstance.getRegisteredClients().containsKey(username) && !controlInstance.getGlobalRegisteredClients().containsKey(username)) {
//			controlInstance.addToBeRegisteredClients(msg, connection);
//			lockAllowedCount.put(username, 0);
//			if (serverList.size() == 0) {
			resultOutput.put("command", "REGISTER_SUCCESS");
			resultOutput.put("info", "register success for " + username);
			connection.writeMsg(resultOutput.toJSONString());
			controlInstance.addRegisteredClients(username, secret);
			return false;
//			}
//			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
//			while (listIterator.hasNext()) {
//				Connection connection1 = listIterator.next();
//				if (connection1.getName().equals(ControlUtil.SERVER)) {
//					JSONObject output = new JSONObject();
//					output.put("command", LOCK_REQUEST);
//					output.put("username", username);
//					output.put("secret", secret);
//					connection1.writeMsg(output.toJSONString());
//					return false;
//				}
//			}
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

	private String processLockRequest(JSONObject msg, Connection connection) throws IOException {
		String username = (String) msg.get("username");
		if (controlInstance.getRegisteredClients().containsKey(username)) {
			return LOCK_DENIED;
		} else {
			broadcastUtil(connection, msg);
			return LOCK_ALLOWED;

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
		return false;
	}

	private boolean activityBroadcastUtil(Connection connection, JSONObject msg) {
		try {
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while (listIterator.hasNext()) {
				Connection connection1 = listIterator.next();
				boolean isSameConnection = (connection1.getSocket().getInetAddress() == connection.getSocket()
						.getInetAddress());
				if (!isSameConnection
						&& ((!connection1.getName().equals(ControlUtil.SERVER) && connection1.isLoggedInClient()) || (connection1.getName().equals
						(ControlUtil.SERVER)))) {
					connection1.writeMsg(msg.toJSONString());
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
		Queue q = controlInstance.getQueue();
		if(q.isEmpty()) {
			controlInstance.setQueue(false);
		}
		if(controlInstance.isQueue()) {
			//add values to Queue
			controlInstance.addQueue(msg);
			for(int i = 0;i< q.size();i++) {
				JSONObject queueMessage = (JSONObject) q.remove();
				ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
				while (listIterator.hasNext()) {
					Connection connection1 = listIterator.next();
					if (connection1.getName().equals(ControlUtil.SERVER) || (connection1.isLoggedInClient())) {
						resultOutput.put("command", "ACTIVITY_BROADCAST");
						resultOutput.put("activity", queueMessage.get("activity"));
						connection1.writeMsg(resultOutput.toJSONString());
					}
				}
			}
			return false;
		}
		if (username.equals("anonymous")) {
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while (listIterator.hasNext()) {
				Connection connection1 = listIterator.next();
				if (connection1.getName().equals(ControlUtil.SERVER) || (connection1.isLoggedInClient())) {
					resultOutput.put("command", "ACTIVITY_BROADCAST");
					resultOutput.put("activity", msg.get("activity"));
					connection1.writeMsg(resultOutput.toJSONString());
				}
			}
			return false;
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
				if (connection1.getName().equals(ControlUtil.SERVER) || (connection1.isLoggedInClient())) {
					resultOutput.put("command", "ACTIVITY_BROADCAST");
					resultOutput.put("activity", activity);
					connection1.writeMsg(resultOutput.toJSONString());
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
				if (null != object) {
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
		if(con.isChild()) {
			//Loop thru and establish new connection to its parent.
			Socket newServer = getSocketDetails(controlInstance.getParentServerId());
			try {
				controlInstance.outgoingConnection(newServer);
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		for (Connection connection : controlInstance.getConnections()) {
			if (connection.isOpen() && connection.getName().equals(ControlUtil.SERVER)) {
				try {
					JSONObject output = new JSONObject();
					output.put("command", "SERVER_BROKEN");
					output.put("serverId", con.getConnectedServerId());				
					controlInstance.setQueue(true);
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
	
	private boolean handleAuthSuccess(JSONObject msg, Connection connection) {
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
		return false;
	}
	
	private Socket getSocketDetails(String serverId) {
		JSONObject serverDetails = serverList.get(serverId);
		Socket newSocket = null;
		String parentId = (String) serverDetails.get("parentId");
		if(parentId != null) {
			try {
				newSocket = new Socket((String) serverDetails.get("parentServerName"), (Integer) serverDetails.get("parentServerPort"));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				newSocket = getSocketDetails(parentId);
				e.printStackTrace();
			} 
		}else {
			//return some child or adjacent server
			
		}
		return newSocket;
	}
}
