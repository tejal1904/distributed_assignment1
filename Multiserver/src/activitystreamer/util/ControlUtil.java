package activitystreamer.util;

import activitystreamer.client.ClientPojo;
import activitystreamer.server.Connection;
import activitystreamer.server.ServerPojo;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import activitystreamer.server.Connection;

public class ControlUtil {

	public static final String REGISTER = "REGISTER";
	public static final String AUTHENTICATION = "AUTHENTICATION";
	public static final String LOGIN = "LOGIN";
	public static final String ACTIVITY_MESSAGE = "ACTIVITY_MESSAGE";
	public static final String SERVER_ANNOUNCE = "SERVER_ANNOUNCE";
	public static final String LOGOUT = "LOGOUT";
	public static final String ACTIVITY_BROADCAST = "ACTIVITY_BROADCAST";
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
			switch(command) {
				case ControlUtil.REGISTER:
					boolean isPresent = false;
					log.info("Register started");
                    connection.writeMsg("SUCCESS!");
					String username = (String) msg.get("username");
					String secret = (String) msg.get("secret");
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
					return true;
				case ControlUtil.LOGIN:
					//Login functionality
					processLogin(msg);
					return true;
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
			
	public boolean processLogin(JSONObject msg) {
		String username = (String) msg.get("username");
		String password = (String) msg.get("password");
		if(checkUsername(username) && checkPassword(password)) {
			return true;
		}else {
			return false;
		}
	}

	public boolean checkUsername(String username) {
//		Control.getInstance()
		return true;
	}

	public boolean checkPassword(String password) {
		return true;
	}
}
