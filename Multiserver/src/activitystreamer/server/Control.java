package activitystreamer.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import activitystreamer.util.ControlUtil;
import activitystreamer.util.Settings;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static final String SERVER = "SERVER";
	private static CopyOnWriteArrayList<Connection> connections;
	private Map<String, String> registeredClients;
	private Map<JSONObject, Connection> toBeRegisteredClients;
	private Map<String, String> globalRegisteredClients;
	private String parentServerId = null;
	private static boolean term = false;
	private static Listener listener;
	protected static Control control = null;
	private PrintWriter outwriter;
	private int load;
	private boolean isQueue = false;
	private Queue<JSONObject> q = new LinkedList<>();

	public boolean isQueue() {
		return isQueue;
	}

	public void setQueue(boolean isQueue) {
		this.isQueue = isQueue;
	}
	
	public void addQueue(JSONObject msg) {
		q.add(msg);
	}
	
	public Queue getQueue() {
		return q;
	}

	public static Control getInstance() {
		if (control == null) {
			control = new Control();
		}
		return control;
	}

	public Control() {
		// initialize the connections array
		connections = new CopyOnWriteArrayList<Connection>();
		registeredClients = new ConcurrentHashMap<>();
		toBeRegisteredClients = new ConcurrentHashMap<>();
		globalRegisteredClients = new ConcurrentHashMap<>();
		// start a listener
		try {
			listener = new Listener();
		} catch (IOException e1) {
			log.fatal("failed to startup a listening thread: " + e1);
			System.exit(-1);
		}
	}

	public void initiateConnection() {
		// make a connection to another server if remote hostname is supplied
		if (Settings.getRemoteHostname() != null) {
			try {
				outgoingConnection(new Socket(Settings.getRemoteHostname(), Settings.getRemotePort()));
			} catch (IOException e) {
				log.error("failed to make connection to " + Settings.getRemoteHostname() + ":"
						+ Settings.getRemotePort() + " :" + e);
				System.exit(-1);
			}
		}
	}

	/*
	 * Processing incoming messages from the connection. Return true if the
	 * connection should close.
	 */
	public synchronized boolean process(Connection con, String msg) {
		return ControlUtil.getInstance().processCommands(con, msg);
	}

	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con) {
		if (!term) {
			connections.remove(con);
		}
	}

	/*
	 * A new incoming connection has been established, and a reference is returned
	 * to it
	 */
	public synchronized Connection incomingConnection(Socket s) throws IOException {
		log.debug("incomming connection: " + Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		log.info(connections.get(0).getSocket().getInputStream().toString());
		return c;
	}

	/*
	 * A new outgoing connection has been established, and a reference is returned
	 * to it
	 */
	@SuppressWarnings("unchecked")
	public synchronized Connection outgoingConnection(Socket s) throws IOException {
		log.debug("outgoing connection: " + Settings.socketAddress(s));
		Connection c = new Connection(s);
		c.setName(Control.SERVER);		
		
		connections.add(c);
		
		DataOutputStream out = new DataOutputStream(s.getOutputStream());
		outwriter = new PrintWriter(out, true);
		JSONObject newCommand = new JSONObject();
		newCommand.put("command", "AUTHENTICATE");
		newCommand.put("secret", Settings.getSecret());
		newCommand.put("id", Settings.getId());
		
		outwriter.println(newCommand);
		outwriter.flush();
		return c;
	}

	@Override
	public void run() {
		log.info("using activity interval of " + Settings.getActivityInterval() + " milliseconds");
		while (!term) {
			// do something with 5 second intervals in between
			try {
				Thread.sleep(Settings.getActivityInterval());
			} catch (InterruptedException e) {
				log.info("received an interrupt, system is shutting down");
				break;
			}
			if (!term) {
				log.debug("doing activity");
				term = doActivity();
			}
		}
		log.info("closing " + connections.size() + " connections");
		// clean up
		for (Connection connection : connections) {
			connection.closeCon();
		}
		listener.setTerm(true);
	}

	@SuppressWarnings("unchecked")
	public boolean doActivity() {
		load = 0;
		for(Connection connection: Control.connections){
			if(!connection.getName().equals(SERVER)){
				load++;
			}
		}
		for (Connection connection : Control.connections) {
			if (connection.isOpen() && connection.getName().equals(Control.SERVER)) {
				try {
					JSONObject output = new JSONObject();
					output.put("command", "SERVER_ANNOUNCE");
					output.put("load", load);
					output.put("hostname", Settings.getLocalHostname());
					output.put("port", Settings.getLocalPort());
					output.put("id", Settings.getId());
					output.put("clientList", getRegisteredClients());
					output.put("parentServerName",Settings.getRemoteHostname());
					output.put("parentServerPort", Settings.getRemotePort());
					output.put("parentId", getParentServerId());
					connection.writeMsg(output.toJSONString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	public final void setTerm(boolean t) {
		term = t;
	}

	public final CopyOnWriteArrayList<Connection> getConnections() {
		return connections;
	}

	public int getLoad() {
		return load;
	}

	public void addRegisteredClients(String username, String secret) {
		registeredClients.put(username, secret);
	}

	public Map<String, String> getRegisteredClients() {
		return registeredClients;
	}

	public void addToBeRegisteredClients(JSONObject username, Connection secret) {
		toBeRegisteredClients.put(username, secret);
	}

	public Map<JSONObject, Connection> getToBeRegisteredClients() {
		return toBeRegisteredClients;
	}

	public Map<String, String> getGlobalRegisteredClients() {
		return globalRegisteredClients;
	}

	public void addGlobalRegisteredClients(String username, String secret) {
		globalRegisteredClients.put(username, secret);
	}

	public String getParentServerId() {
		return parentServerId;
	}

	public void setParentServerId(String parentServerId) {
		this.parentServerId = parentServerId;
	}
	
	

}
