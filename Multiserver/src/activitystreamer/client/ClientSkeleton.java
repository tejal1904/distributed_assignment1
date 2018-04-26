package activitystreamer.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.util.Settings;

public class ClientSkeleton extends Thread {
	private static ClientSkeleton clientSolution;
	private TextFrame textFrame;
	private PrintWriter outwriter;
	private BufferedReader inReader;
	private Socket socket;

	public static ClientSkeleton getInstance() {
		if (clientSolution == null) {
			clientSolution = new ClientSkeleton();
		}
		return clientSolution;
	}

	public ClientSkeleton() {
		try {
			socket = new Socket(Settings.getRemoteHostname(), Settings.getLocalPort());
			inReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			outwriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            loginClient();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		start();
	}

	public void sendActivityObject(JSONObject activityObj) {
		outwriter.println(activityObj.toString());
		outwriter.flush();
	}

	public void disconnect() {
		try {
			Thread.sleep(2000);
			socket.close();
			textFrame.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private synchronized void loginClient(){
        JSONObject object = new JSONObject();
        object.put("command","LOGIN");
        object.put("username",Settings.getUsername());
        object.put("secret", Settings.getSecret());
        sendActivityObject(object);
    }
    private synchronized void registerClient(){
        JSONObject object = new JSONObject();
        object.put("command","REGISTER");
        object.put("username",Settings.getUsername());
        object.put("secret", Settings.getSecret());
        sendActivityObject(object);
    }

	public void run() {
		String message;
		try {
			while ((message = inReader.readLine()) != null) {
				System.out.println(message.toString());
				JSONParser parser = new JSONParser();
				JSONObject outputJson = (JSONObject) parser.parse(message);
				if(outputJson.get("command").equals("LOGIN_FAILED")){
					socket = new Socket(Settings.getRemoteHostname(), Settings.getLocalPort());
					outwriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
					inReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
					registerClient();
                }else if(outputJson.get("command").equals("LOGIN_SUCCESS")){
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if((message = inReader.readLine()) != null) {
						outputJson = (JSONObject) parser.parse(message);
						if(outputJson.get("command").equals("REDIRECT")) {
							
//							socket.close();
							try {
								socket = new Socket((String) outputJson.get("hostname"), Integer.valueOf(outputJson.get("port").toString()));
								outwriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
								inReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
								loginClient();
							} catch (UnknownHostException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}else {
							textFrame = new TextFrame();
						}
					}
					
                }else if(outputJson.get("command").equals("REGISTER_FAILED")){
                    System.out.println(outputJson.toJSONString());
					inReader.close();
					outwriter.close();
					socket.close();
                }else if(outputJson.get("command").equals("INVALID_MESSAGE")){
                    System.out.println(outputJson.toJSONString());
                    inReader.close();
                    outwriter.close();
                    socket.close();
                }else if(outputJson.get("command").equals("REGISTER_SUCCESS")){
                    loginClient();
                }else {
                    textFrame.setOutputText(outputJson);
                }
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
