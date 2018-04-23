package activitystreamer.util;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.server.ServerPojo;
import java.io.IOException;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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

	public String result_command = "";
	public String result_info = "";
	JSONObject resultOutput = new JSONObject();
	JSONParser parser = new JSONParser();
	ServerPojo serverPojo = ServerPojo.getInstance();
	Control controlInstance = Control.getInstance();
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
                    String username = (String) msg.get("username");
                    int clientSize = controlInstance.getRegisteredClients().size();
				    controlInstance.addRegisteredClients(username,secret);
                    if((clientSize + 1) == controlInstance.getRegisteredClients().size()){
                        connection.setClient(true);
                        resultOutput.put("command", "REGISTER_SUCCESS");
                        connection.writeMsg(resultOutput.toJSONString());
                        // send lock request to all other servers
						for(Connection connection1:controlInstance.getConnections()){
							if(!connection1.isClient() && !connection1.isParentServer()){
								JSONObject output = new JSONObject();
								output.put("command",LOCK_REQUEST);
								output.put("username",username);
								output.put("secret",secret);
								connection1.writeMsg(output.toJSONString());
							}
						}
                        return false;
                    }else if(connection.isLoggedInClient() == true) {
                        resultOutput.put("command", "INVALID_MESSAGE");
                        resultOutput.put("info","Client already logged in to the system");
                        connection.writeMsg(resultOutput.toJSONString());
                        return true;

                    }else{
                        resultOutput.put("command", "REGISTER_FAILED");
                        resultOutput.put("info",username + " is already registered with the system");
                        connection.writeMsg(resultOutput.toJSONString());
                        return true;
                    }

				case ControlUtil.AUTHENTICATION:
					String info = authenticateServer(connection,msg);
					if(info.equals("SUCCESS")){
						return false;
					}else if(info.equals("INVALID_MESSAGE")){
						resultOutput.put("command",info);
						resultOutput.put("info","Server already successfully authenticated");
						connection.writeMsg(resultOutput.toJSONString());
						return true;
					}else if(info.equals("AUTHENTICATION_FAIL")){
						resultOutput.put("command",info);
						resultOutput.put("info","the supplied secret is incorrect: " + secret);
						connection.writeMsg(resultOutput.toJSONString());
						return true;
					}
				case ControlUtil.LOGIN:
				    return loginUtil(connection, msg);
				case ControlUtil.LOGOUT:
				    connection.setLoggedInClient(false);
					return true;
				case ControlUtil.ACTIVITY_MESSAGE:
					return activityMessageUtil(connection, msg);
				case ControlUtil.ACTIVITY_BROADCAST:
                    connection.setClient(false);
					return activityBroadcastUtil(connection, msg);
				case ControlUtil.SERVER_ANNOUNCE:
				    connection.setClient(false);
					System.out.println("Received!");
					return serverAnnounce(connection,msg);
				case ControlUtil.LOCK_REQUEST:
					//process received lock request
					String data = processLockRequest(connection,msg);
					//TODO: need to send the lock_allowed and lock_denied commands to all others servers
					//TODO: write code to receive lock_allowed and lock_denied
					if(data.equals(LOCK_ALLOWED)){
						return false;
					}else if(data.equals(LOCK_DENIED)){
						return true;
					}

				default:
					resultOutput.put("command", "INVALID_MESSAGE");
					resultOutput.put("info", "The received message did not contain a command");
					connection.writeMsg(resultOutput.toJSONString());
					return true;
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
            e.printStackTrace();
        }
		return false;

	}

	private String processLockRequest(Connection connection, JSONObject msg) {
		String username = (String) msg.get("username");
		if(controlInstance.getRegisteredClients().containsKey(username)){
			return LOCK_DENIED;
		}else {
			return LOCK_ALLOWED;
		}
	}

	private String authenticateServer(Connection connection, JSONObject msg) {
		String username = (String) msg.get("username");
		String secret = (String) msg.get("secret");
		for(Connection connection1:controlInstance.getConnections()){
			if(connection1.getSocket().getInetAddress().equals(connection.getSocket().getInetAddress()) && connection1.getSocket().getPort() ==
					connection.getSocket().getPort()){
				return "INVALID_MESSAGE";
			}
		}
		if(Settings.getSecret().equals(secret)){
			connection.setClient(false);
			return "SUCCESS";
		}else {
			return "AUTHENTICATION_FAIL";
		}
	}

	private boolean serverAnnounce(Connection connection, JSONObject msg) {
	    return false;
    }

    private boolean activityBroadcastUtil(Connection connection, JSONObject msg) {
		try {
			for(Connection con : controlInstance.getConnections()){
			    if((con.isClient() && con.isLoggedInClient()) || (!con.isClient() && !con.isParentServer())){
                    con.writeMsg(msg.toJSONString());
                }
            }

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	private boolean activityMessageUtil(Connection connection, JSONObject msg) throws IOException {
        connection.setClient(true);
        String username = (String) msg.get("username");
        String secret = (String) msg.get("secret");
        if(username.equals("anonymous")){
            for(Connection connection1 : controlInstance.getConnections()){
                if(!connection1.isClient()){
                    resultOutput.put("command","ACTIVITY_BROADCAST");
                    resultOutput.put("activity",msg.get("activity"));
                    connection1.writeMsg(resultOutput.toJSONString());
                }
            }
            return false;
        }


        if((!connection.isLoggedInClient()) || (!(controlInstance.getRegisteredClients().containsKey(username) &&
                controlInstance.getRegisteredClients().get(username).equals(secret)))){
            resultOutput.put("command","AUTHENTICATION_FAIL");
            connection.writeMsg(resultOutput.toJSONString());
            return true;
        }else {
           for(Connection connection1 : controlInstance.getConnections()){
               if(!connection1.isClient()){
                   resultOutput.put("command","ACTIVITY_BROADCAST");
                   resultOutput.put("activity",msg.get("activity"));
                   connection1.writeMsg(resultOutput.toJSONString());
               }
           }
            return false;
        }

	}

	@SuppressWarnings("unchecked")
	private boolean loginUtil(Connection connection, JSONObject msg) throws IOException {
		String secret;
		String username;
		username = (String) msg.get("username");
		secret = (String) msg.get("secret");
		String info = checkCredentials(username,secret);
		if(info.equals("success")) {
            connection.setClient(true);
            connection.setLoggedInClient(true);
			resultOutput.put("command", "LOGIN_SUCCESS");
			resultOutput.put("info", "logged in as user " + username);
			connection.writeMsg(resultOutput.toJSONString());
			return false;
		}else {
			resultOutput.put("command", "LOGIN_FAILED");
			resultOutput.put("info",info);
			connection.writeMsg(resultOutput.toJSONString());
			return true;
		}
	}

	public String checkCredentials(String username,String password) {
	    Map registeredUsers = controlInstance.getRegisteredClients();
		if((registeredUsers.containsKey(username) && (registeredUsers.get(username).equals(password))) || username.equals("anonymous")){
		    return "success";
        }else if(registeredUsers.containsKey(username) && (!registeredUsers.get(username).equals(password))){
		    return "attempt to login with wrong secret";
        } else if(!registeredUsers.containsKey(username)){
            return "client is not registered with the server";
        }else{
            return "";
        }
	}
	
	private void connectServer(String message) {
		System.out.println("connected & need to implement authenticate msg");
	}
}
