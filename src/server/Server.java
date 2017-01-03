package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
	private ServerSocket listenSocket;
	private static ArrayList<ClientConnection> connectedClients = new ArrayList<ClientConnection>();
	
	/* Constructor - starts server on the port 'portNum'. */
	public Server(int portNum) {
		try {
			System.out.println("Opening on port "+portNum+"...");
			listenSocket = new ServerSocket(portNum);
			// Listens for console commands.
			(new Thread(new Runnable() {
				public void run() {
					String command = "";
					while((command = System.console().readLine()) != null) {
						switch(command) {
						case "ls":	// Lists all connected clients.
							System.out.println("\r----------- Connected Clients ("+connectedClients.size()+") -----------");
							for(ClientConnection c : connectedClients) {
								System.out.println(c.getUsername());
							}
							System.out.println("-------------------------------------------");
							break;
						default:	// User enters an unknown command.
							System.out.println("Unknown command.");
							break;
						}
					}
				}
			}, "Console Command Handler")).start();
			System.out.println("Ready for connections.");
			// Waits for new clients to connect, then starts a new ClientConnection thread for them.
			while(true) {
				Socket clientConn = listenSocket.accept();
				connectedClients.add(new ClientConnection(clientConn));
			}
		// If the server can't start, close the server socket and exit the program.
		} catch(IOException e) {
			try {if(listenSocket != null)listenSocket.close();} catch (IOException e1) {}
			System.out.println("Could not start server.");
		}
	}
	
	/* Used by the ClientConnection thread to remove it's self from the connected clients list 
	 * when the client disconnects. */
	public static synchronized void removeClient(ClientConnection clientConn) {
		connectedClients.remove(clientConn);
	}
	
	public static void main(String[] args) {
		new Server(31415);
	}
}
