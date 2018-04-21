package activitystreamer.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
	
	public static ClientSkeleton getInstance(){
		if(clientSolution==null){
			clientSolution = new ClientSkeleton();
		}
		return clientSolution;
	}
	
	public ClientSkeleton(){		
		textFrame = new TextFrame();
		start();
	}	
	
	@SuppressWarnings("unchecked")
	public void sendActivityObject(JSONObject activityObj){
		
		try(Socket socket = new Socket(Settings.getRemoteHostname(), Settings.getLocalPort());){
			// Output and Input Stream
			DataInputStream in = new DataInputStream(socket.
					getInputStream());
		    DataOutputStream out = new DataOutputStream(socket.
		    		getOutputStream());
		    PrintWriter outwriter = new PrintWriter(out, true);
		    BufferedReader inReader = new BufferedReader( new InputStreamReader(in));
		    outwriter.println(activityObj.toString());
	    	outwriter.flush();    
			String message = inReader.readLine();
			JSONParser parser = new JSONParser();
			JSONObject outputJson = (JSONObject) parser.parse(message);
			
			System.out.println("message Received from server: "+message);
			textFrame.setOutputText(outputJson);
		    
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}	
	
	public void disconnect(){
		
	}	
	
	public void run(){		

	}
	
}
