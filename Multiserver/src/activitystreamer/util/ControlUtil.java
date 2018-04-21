package activitystreamer.util;

import activitystreamer.client.ClientPojo;
import activitystreamer.server.Connection;
import activitystreamer.server.ServerPojo;
import java.io.IOException;
import java.net.ServerSocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.client.ClientPojo;
import activitystreamer.server.Connection;
import activitystreamer.server.ServerPojo;

public class ControlUtil {

	public static final String REGISTER = "REGISTER";
	public static final String AUTHENTICATION = "AUTHENTICATE";
	public static final String LOGIN = "LOGIN";
	public static final String ACTIVITY_MESSAGE = "ACTIVITY_MESSAGE";
	public static final String SERVER_ANNOUNCE = "SERVER_ANNOUNCE";
	public static final String LOGOUT = "LOGOUT";
	public static final String ACTIVITY_BROADCAST = "ACTIVITY_BROADCAST";
	public String result_command = "";
	public String result_info = "";
	JSONParser parser = new JSONParser();
	ServerPojo serverPojo = ServerPojo.getInstance();
	private static final Logger log = LogManager.getLogger();

	protected static ControlUtil controlUtil = null;
	
	public static ControlUtil getInstance() {
		if(controlUtil==null){
			controlUtil=new ControlUtil();
		} 
		return controlUtil;
	}	
	
	public boolean processCommands(Connection connection, String message) {
		JSONObject msg;
		try {
			msg = (JSONObject) parser.parse(message);		
			String command = (String) msg.get("command");
			String secret = (String) msg.get("secret");
			switch(command) {
				case ControlUtil.REGISTER:
					boolean isPresent = false;
					log.info("Register started");
                    connection.writeMsg("SUCCESS!");
					String username = (String) msg.get("username");

					for(ClientPojo clientPojo : serverPojo.getClientPojoList()){
						if(username.equals(clientPojo.getUsername())){
							isPresent = true;
						}
					}
					if(isPresent){
						System.out.println("Present");
						return false;

					}else {
						ClientPojo clientPojo = new ClientPojo();
						clientPojo.setUsername(username);
						clientPojo.setSecret(secret);
						clientPojo.setSocket(connection.getSocket());
						serverPojo.addClients(clientPojo);
						System.out.println("not present: added: "+clientPojo + "  "+serverPojo);
						System.out.println("size of clients: "+serverPojo.getClientPojoList().size());
						//send lock request to other servers
					}
					break;
				case ControlUtil.AUTHENTICATION:
					//Authentication functionality
					if(secret.equals(serverPojo.getSecret())){
						ServerPojo childServer = new ServerPojo();
						childServer.setSecret(secret);
						childServer.setPort(connection.getSocket().getPort());
						childServer.setHostName(connection.getSocket().getInetAddress()+":"+connection.getSocket().getPort());
						childServer.addParentServer(serverPojo);
						serverPojo.addChildServers(childServer);
						System.out.println("size of child servers: "+serverPojo.getChildServerList().size());
						System.out.println("child server -> "+serverPojo.getChildServerList().get(0).getHostName());
                        System.out.println("parent server -> "+serverPojo.getParentServer().getHostName());
                    }

					return true;
				case ControlUtil.LOGIN:
					//Login functionality
				return loginUtil(connection, msg);
				case ControlUtil.LOGOUT:
					//Logout functionality
					return true;
				case ControlUtil.ACTIVITY_MESSAGE:
					//Activity message functionality
					return true;
				case ControlUtil.ACTIVITY_BROADCAST:
					//Activity message functionality
					return true;
				case ControlUtil.SERVER_ANNOUNCE:
					//Server Announce functionality
					return true;
				default:
					return false;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
            e.printStackTrace();
        }
		return false;

	}

	@SuppressWarnings("unchecked")
	private boolean loginUtil(Connection connection, JSONObject msg) throws IOException {
		String secret;
		String username;
		JSONObject newCommand = new JSONObject();
		username = (String) msg.get("username");
		secret = (String) msg.get("secret");
		if(checkCredentials(username,secret)) {
			newCommand.put("command", "LOGIN_SUCCESS");
			newCommand.put("info", "logged in as user " + username);
			connection.writeMsg(newCommand.toJSONString());
			return true;
		}else {
			
			newCommand.put("command", "LOGIN_FAILED");
			String failureMessage = getFailureMessage(username, secret);
			
			newCommand.put("info",failureMessage);
			connection.writeMsg(newCommand.toJSONString());
			return false;
		}
	}

	public boolean checkCredentials(String username,String password) {
		for(ClientPojo clientPojo : serverPojo.getClientPojoList()){
			if(username.equals(clientPojo.getUsername()) && password.equals(clientPojo.getSecret())){
				return true;
			}
		}
		return false;
	}
	
	public String getFailureMessage(String username,String password) {
		String message = null;
		int count = 0;
		for(ClientPojo clientPojo : serverPojo.getClientPojoList()){
			count++;
			if(username.equals(clientPojo.getUsername()) && !password.equals(clientPojo.getSecret())){
				message = "attempt to login with wrong secret";
			}
		}
		if(count == serverPojo.getClientPojoList().size() && message == null) {
			message = "client is not registered with the server";
		}
		return message;
	}

	private void connectServer(String message) {
		System.out.println("connected & need to implement authenticate msg");
	}
}
