package activitystreamer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ControlUtil {

	public static final String REGISTER = "REGISTER";
	public static final String AUTHENTICATION = "AUTHENTICATION";
	public static final String LOGIN = "LOGIN";
	public static final String ACTIVITY_MESSAGE = "ACTIVITY_MESSAGE";
	public static final String SERVER_ANNOUNCE = "SERVER_ANNOUNCE";
	public static final String LOGOUT = "LOGOUT";
	public static final String ACTIVITY_BROADCAST = "ACTIVITY_BROADCAST";
	JSONParser parser = new JSONParser();
	private static final Logger log = LogManager.getLogger();

	protected static ControlUtil controlUtil = null;
	
	public static ControlUtil getInstance() {
		if(controlUtil==null){
			controlUtil=new ControlUtil();
		} 
		return controlUtil;
	}	
	
	public void processCommands(String message) {
		JSONObject msg;
		try {
			msg = (JSONObject) parser.parse(message);		
			String command = (String) msg.get("command");
			switch(command) {
				case ControlUtil.REGISTER:
					//TODO register functionality
					log.debug("Register started");
					break;
				case ControlUtil.AUTHENTICATION:
					//Authentication functionality
					break;
				case ControlUtil.LOGIN:
					//Login functionality
					break;
				case ControlUtil.LOGOUT:
					//Logout functionality
					break;
				case ControlUtil.ACTIVITY_MESSAGE:
					//Activity message functionality
					break;
				case ControlUtil.ACTIVITY_BROADCAST:
					//Activity message functionality
					break;	
				case ControlUtil.SERVER_ANNOUNCE:
					//Server Announce functionality
					break;			
			}
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
			

}
