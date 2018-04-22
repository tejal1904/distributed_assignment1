package activitystreamer.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import activitystreamer.util.Settings;

public class ClientSkeleton extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ClientSkeleton clientSolution;
	private TextFrame textFrame;
	private PrintWriter outwriter;
	private BufferedReader inReader;

	public static ClientSkeleton getInstance() {
		if (clientSolution == null) {
			clientSolution = new ClientSkeleton();
		}
		return clientSolution;
	}

	public ClientSkeleton() {
		textFrame = new TextFrame();
		try {
			Socket socket = new Socket(Settings.getRemoteHostname(), Settings.getLocalPort());
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
			;

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
