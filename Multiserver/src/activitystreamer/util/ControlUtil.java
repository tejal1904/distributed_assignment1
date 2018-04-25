package activitystreamer.util;

import activitystreamer.server.Connection;
import activitystreamer.server.Control;
import activitystreamer.server.ServerPojo;
import java.io.IOException;
import java.util.*;

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
	public static Map<String, Integer> lockAllowedCount = new HashMap<>();
	public static Map<String,JSONObject> serverList = new HashMap<>();

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
	
	@SuppressWarnings("unchecked")
	public boolean processCommands(Connection connection, String message) {
		JSONObject msg;
		try {
			msg = (JSONObject) parser.parse(message);		
			String command = (String) msg.get("command");
            String secret = (String) msg.get("secret");
			if(command == null ) {
				resultOutput.put("command", "INVALID_MESSAGE");
				resultOutput.put("info", "The received message did not contain a command");
				connection.writeMsg(resultOutput.toJSONString());
				return true;
			}
			switch(command) {
				case ControlUtil.REGISTER:
                    String username = (String) msg.get("username");
                    if(!controlInstance.getRegisteredClients().containsKey(username)){
						controlInstance.addToBeRegisteredClients(msg,connection);
						lockAllowedCount.put(username,0);
						if(serverList.size() < 2) {
							resultOutput.put("command", "REGISTER_SUCCESS");
							resultOutput.put("info","register success for "+username);
							connection.writeMsg(resultOutput.toJSONString());
							controlInstance.addRegisteredClients(username,secret);
							return false;
						}
						ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
						while(listIterator.hasNext()){
							Connection connection1 = listIterator.next();
							if(!connection1.isClient()){
								JSONObject output = new JSONObject();
								output.put("command",LOCK_REQUEST);
								output.put("username",username);
								output.put("secret",secret);
								connection1.writeMsg(output.toJSONString());
							}
						}
					}else{
						resultOutput.put("command", "REGISTER_FAILED");
						resultOutput.put("info",username + " is already registered with the system");
						connection.writeMsg(resultOutput.toJSONString());
						return true;
					}
					if(connection.isLoggedInClient() == true) {
                        resultOutput.put("command", "INVALID_MESSAGE");
                        resultOutput.put("info","Client already logged in to the system");
                        connection.writeMsg(resultOutput.toJSONString());
                        return true;
                    }
					return false;
				case ControlUtil.AUTHENTICATION:
					String info = authenticateServer(connection,msg);
					if(info.equals("SUCCESS")){
						return false;
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
					return serverAnnounce(msg);
				case ControlUtil.LOCK_REQUEST:
					//process received lock request
					String username3 = (String) msg.get("username");
					String data = processLockRequest(msg);
					if(data.equals(LOCK_ALLOWED)){
						ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
						while(listIterator.hasNext()){
							Connection connection1 = listIterator.next();
							if(!connection1.isClient()){
								resultOutput.put("command",data);
								resultOutput.put("username", username3);
								resultOutput.put("secret", secret);
								connection1.writeMsg(resultOutput.toJSONString());
								return false;
							}
						}
					}else if(data.equals(LOCK_DENIED)){
						ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
						while(listIterator.hasNext()){
							Connection connection1 = listIterator.next();
							if(!connection1.isClient()){
								resultOutput.put("command",data);
								resultOutput.put("username", username3);
								resultOutput.put("secret", secret);
								connection1.writeMsg(resultOutput.toJSONString());
								return false;
							}
						}
					}
				case  ControlUtil.LOCK_ALLOWED:
					String username1 = (String) msg.get("username");
					int count=0;
					if(null != lockAllowedCount.get(username1)){
						count = lockAllowedCount.get(username1);
					}
					lockAllowedCount.put(username1, count+1);
					int totalServers = 0;
					ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
					while(listIterator.hasNext()){
						Connection connection1 = listIterator.next();
						if(!connection1.isClient()){
							totalServers++;
						}
					}
					if(totalServers == lockAllowedCount.get(username1)){
						Iterator<JSONObject> iterator = controlInstance.getToBeRegisteredClients().keySet().iterator();
						while(iterator.hasNext()){
							JSONObject object = iterator.next();
							Connection connection1 = controlInstance.getToBeRegisteredClients().get(object);
							if(null != object && null != connection1){
								if(username1.equals(object.get("username").toString())){
									lockAllowedCount.remove(username1);
									controlInstance.addRegisteredClients(username1,object.get("secret").toString());
									resultOutput.put("command","REGISTER_SUCCESS");
									resultOutput.put("info","register success for "+username1);
									connection1.writeMsg(resultOutput.toJSONString());
									return false;
								}
							}
						}
					}
					return false;

				case ControlUtil.LOCK_DENIED:
					String username2 = (String) msg.get("username");
					JSONObject object = null;
					Connection connection1 = null;
					for(Map.Entry<JSONObject,Connection> entry:controlInstance.getToBeRegisteredClients().entrySet()){
						if(username2.equals(entry.getKey().get("username").toString())){
							object = entry.getKey();
							connection1 = entry.getValue();
						}
					}
					if(null != connection1 && null != object){
						controlInstance.getToBeRegisteredClients().remove(object);
						lockAllowedCount.remove(username2);
						resultOutput.put("command", "REGISTER_FAILED");
						resultOutput.put("info", object.get("username").toString() + " is already registered with the system");
						connection1.writeMsg(resultOutput.toJSONString());
					}
					return false;

				default:
					resultOutput.put("command", "INVALID_MESSAGE");
					resultOutput.put("info", "Invalid command");
					connection.writeMsg(resultOutput.toJSONString());
					return true;
			}
		} catch (ParseException e) {
				resultOutput.put("command","INVALID_MESSAGE");
				resultOutput.put("info","JSON parse error while parsing message");
				try {
					connection.writeMsg(resultOutput.toJSONString());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				return true;
		}catch (IOException e) {
            e.printStackTrace();
        }
		return false;

	}

	private String processLockRequest(JSONObject msg) {
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

		if(Settings.getSecret().equals(secret)){
			connection.setClient(false);
			return "SUCCESS";
		}else {
			return "AUTHENTICATION_FAIL";
		}
	}

	private boolean serverAnnounce(JSONObject msg) throws IOException {
		if(null != msg.get("id")){
			serverList.put((String)msg.get("id"), msg);
		}
		return false;
    }

    private boolean activityBroadcastUtil(Connection connection, JSONObject msg) {
		try {
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while(listIterator.hasNext()){
				Connection connection1 = listIterator.next();
				boolean isSameConnection = (connection1.getSocket().getInetAddress() == connection.getSocket().getInetAddress());
			    if(!isSameConnection && ((connection1.isClient() && connection1.isLoggedInClient()) || (!connection1.isClient()))){
					connection1.writeMsg(msg.toJSONString());
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
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while(listIterator.hasNext()){
				Connection connection1 = listIterator.next();
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
			ListIterator<Connection> listIterator = controlInstance.getConnections().listIterator();
			while(listIterator.hasNext()){
				Connection connection1 = listIterator.next();
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
		String username1;
		username1 = (String) msg.get("username");
		secret = (String) msg.get("secret");
		String info = checkCredentials(username1,secret);
		if(info.equals("success")) {
            connection.setClient(true);
            connection.setLoggedInClient(true);
			resultOutput.put("command", "LOGIN_SUCCESS");
			resultOutput.put("info", "logged in as user " + username1);
			connection.writeMsg(resultOutput.toJSONString());
			Iterator<String> stringIterator = serverList.keySet().iterator();
			while (stringIterator.hasNext()){
				String object = stringIterator.next();
				if(null != object){
					if(controlInstance.getLoad() > ((Long)serverList.get(object).get("load")).intValue() + 2) {
						try {
							connection.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						resultOutput.put("command", "REDIRECT");
						resultOutput.put("hostname",(String) serverList.get(object).get("hostname"));
						resultOutput.put("port", String.valueOf(serverList.get(object).get("port")));
						connection.writeMsg(resultOutput.toJSONString());
						return true;
					}
				}
			}			
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
