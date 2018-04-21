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
	public static final String AUTHENTICATION = "AUTHENTICATION";
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
						//send lock request to other servers
					}
					break;
				case ControlUtil.AUTHENTICATION:
					//Authentication functionality
					if(secret.equals(serverPojo.getSecret())){
						ServerPojo childServer = new ServerPojo();
						childServer.setSecret(secret);
						childServer.setSocket(new ServerSocket(connection.getSocket().getPort()));
						childServer.addParentServer(serverPojo);
						serverPojo.addChildServers(childServer);
						System.out.println("Authenticated new server -> "+serverPojo);
					}

					return true;
				case ControlUtil.LOGIN:
					//Login functionality
					username = (String) msg.get("username");
					secret = (String) msg.get("password");
					if(checkCredentials(username,secret)) {
						connection.writeMsg("LOGIN_SUCCESS");
						return true;
					}else {
						//call failure model
						return false;
					}
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

	public boolean checkCredentials(String username,String password) {
		for(ClientPojo clientPojo : serverPojo.getClientPojoList()){
			if(username.equals(clientPojo.getUsername())){
				if(password.equals(clientPojo.getSecret())){
					return true;
				}
			}
		}
		return false;
	}

	public boolean checkPassword(String password) {
		return true;
	}

	private void connectServer(String message) {
		System.out.println("connected & need to implement authenticate msg");
	}
}
