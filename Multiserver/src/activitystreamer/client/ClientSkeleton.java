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
			inReader.close();
			outwriter.close();
			socket.close();
		} catch (IOException e) {
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
				textFrame.setOutputText(outputJson);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

}
