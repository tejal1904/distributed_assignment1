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
		textFrame = new TextFrame();
		try {
			socket = new Socket(Settings.getRemoteHostname(), Settings.getLocalPort());
			inReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
			outwriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
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

	public void run() {
		String message;
		try {
			while ((message = inReader.readLine()) != null) {
				JSONParser parser = new JSONParser();
				JSONObject outputJson = (JSONObject) parser.parse(message);
				System.out.println("message Received from server: " + message);
				if(outputJson.get("command").equals("REDIRECT")) {
					socket.close();
					try {
						Thread.sleep(1000);
						socket = new Socket((String) outputJson.get("hostname"), (int) outputJson.get("port"));
						outwriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
						JSONObject reLogin = new JSONObject();
						reLogin.put("command", "LOGIN");
						reLogin.put("username", Settings.getUsername());
						reLogin.put("secret", Settings.getSecret());
						outwriter.println(reLogin.toJSONString());
						outwriter.flush();
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				textFrame.setOutputText(outputJson);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
