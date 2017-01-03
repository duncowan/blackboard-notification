package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {
	public static boolean isWindows;
	
	private final int PORT;
	private final String ADDRESS;
	
	private Socket socket;
	private DataOutputStream out;
	private DataInputStream in;

	/* Constructor - sets port and address, then attempts to connect to server. */
	public Client(int port, String address) {
		this.PORT = port;
		this.ADDRESS = address;
		this.connect();
		this.closeConnections();
	}

	/* Attempts to connect to server and sets up the data input/output streams. */
	private void connect() {
		this.closeConnections();	// Make sure any previous connections are closed.
		// Try to connect to connect to server. 
		// If connection fails, wait 5 seconds and try again.
		while(true) {
			try {
				socket = new Socket(ADDRESS, PORT);
				break;
			} catch(IOException e) {
				try {Thread.sleep(5000);} catch (Exception e1) {}
				System.out.println("Could not connect. Trying again...");
			}
		}

		// Try to set up input/output streams to server. 
		// If one or both fail, try to reconnect to server.
		try {
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
		} catch(IOException e) {
			System.out.println("Could not open streams. Reconnecting...");
			this.connect();
		}
		System.out.println(socket.getLocalPort());
		this.login();
	}
	
	/* Passes client's login information to server. */
	private void login() {
		Data.load();	// Loads client's login information.
		// If the client's login information hasen't been set 
		// or there is a problem reading the data file, close all connections and exit the program.
		if(Data.getValue("username") == null || Data.getValue("password") == null) {
			System.out.println("No username and/or password set. Exiting.");
			this.closeConnections();
			return;
		}
		
		// Send client's login information an wait for reply.
		// If the client's information can't be sent, try to reconnect to server.
		try {
			out.writeUTF(Data.getValue("username"));
			out.writeUTF(Data.getValue("password"));
			System.out.println("Connecting...");
			this.listen();
		} catch (IOException e) {
			System.out.println("Could not send user data. Reconnecting...");
			this.connect();
		}
	}
	
	/* Listens for and processes data sent from the server. */
	private void listen() {
		String data;
		NotificationGUI noteGUI = null;
		try {
			// While the input stream is open, get and process data from server.
			while((data = in.readUTF()) != null) {
				// If the previously sent login information was incorrect,
				// close all connections and exit the program.
				if(data.startsWith("-c0"))
					break;
				else if(data.startsWith("-c1"));	// REMOVE
				
				// Display any messages/notifications to the client.
				else {
					// If there's already a notification GUI open, close it.
					if(noteGUI != null && noteGUI.isDisplayable())
						noteGUI.dispose();
					// Every message or notification must start with either a '-n' or '-m'
					// if it's to be sent to the notification GUI.
					if(data.length() > 2)
						noteGUI = new NotificationGUI(data);
					// ...Any other data will be sent back to the server.
					// (used for ping pong connection test)
					else
						out.writeUTF(data);
				}
			}
		// If the connection stream is closed, try to reconnect to server.
		} catch (IOException e) {
			System.out.println("Lost connection to server. Attempting to reconnect...");
			this.connect();
		}
	}
	
	/* Close all connections to server. */
	private void closeConnections() {
		try {
			if(in != null) in.close();
			if(out != null) out.close();
			if(socket != null) socket.close();
		} catch(IOException e) {
			System.out.println("Could not close connections. Continuing.");
		}
	}

	public static void main(String[] args) {
		// Fixes error on Mac when the notification GUI is displayed.
		if(System.getProperty("os.name").toLowerCase().startsWith("mac"))
			System.setProperty("apple.awt.UIElement", "true");
		// Used to position close button on Windows to the top right instead of top left (Linux/Mac).
		else if(System.getProperty("os.name").toLowerCase().startsWith("windows"))
			isWindows = true;
		new Client(31415, (args.length < 1 ? "localhost" : args[0]));
	}
}
