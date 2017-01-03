package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;

import org.apache.commons.logging.LogFactory;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class ClientConnection extends Thread {
	private Socket clientConn;
	private DataOutputStream out;
	private DataInputStream in;
	private WebClient webClient;
	private HtmlPage studentHome;

	private String user;
	public volatile boolean running = true;

	public ClientConnection (Socket clientConn) {
		this.clientConn = clientConn;
		this.user = clientConn.getRemoteSocketAddress().toString().replace("/", "");
		this.start();
	}

	/* Opens input and output streams. */
	private boolean setupStreams() {
		try {
			out = new DataOutputStream(this.clientConn.getOutputStream());
			in = new DataInputStream(this.clientConn.getInputStream());
		} catch(IOException e) {
			System.out.println("Could not open streams to "+this.user+".");
			this.terminate();
			return false;
		}
		return true;
	}

	/* Returns the name of this client. (Used when the user enters the 'ls' command). */
	public synchronized String getUsername() {
		return this.user;
	}

	public void run() {
		// Suppresses error messages form HtmlUnit.
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		String pass = "";

		// Set up input/output streams.
		if(!this.setupStreams())
			return;
		
		System.out.println(clientConn.getPort());

		// Tries to get username and password from client.
		try {
			this.user = in.readUTF();
			pass = in.readUTF();
		} catch (IOException e) {
			System.out.println("Did not get username and/or password from "+this.user+".");
			this.terminate();
			return;
		}

		System.out.println(this.user+ " has connected. [" + new Timestamp(System.currentTimeMillis())+"]");
		this.setName("<"+this.user+"> Main Thread");

		// Open web client and, disable css and JavaScript to speed up login process.
		webClient = new WebClient();
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setJavaScriptEnabled(false);

		// Try to get Blackboard login page.
		HtmlPage loginPage = null;
		try {
			loginPage = webClient.getPage("https://blackboard.otago.ac.nz/");
		} catch(Exception e){
			System.out.println("<"+this.user+"> Could not connect to Blackboard.");
			this.terminate();
			return;
		}

		// Find the login form.
		final HtmlForm form = loginPage.getFormByName("login");
		// Get the username, password and submit inputs from the login form.
		final HtmlTextInput username = form.getInputByName("user_id");
		final HtmlPasswordInput password = form.getInputByName("password");
		final HtmlSubmitInput loginButton = form.getInputByName("login");

		// Enter username and password from client into login form.
		username.setValueAttribute(this.user);
		password.setValueAttribute(pass);

		// Try to login to Blackboard.
		try {
			studentHome = loginButton.click();
			if(studentHome.getElementById("badgeTotalCount") == null) {
				out.writeUTF("-mUnable to connect. Incorrect password.");
				out.writeUTF("-c0");
				this.terminate();
				return;
			} else {
				//out.writeUTF("-mConnected!");
				out.writeUTF("-c1");
			}
		} catch(IOException e) {
			System.out.println("<"+this.user+"> Could not login to Blackboard.");
			this.terminate();
			return;
		}
		webClient.getOptions().setJavaScriptEnabled(true);	// Re-enable JavaScript to get notifications.

		boolean sendData = false;
		String lastNum = "0";
		int zeroCount = 0;
		Thread ping = null;
		// Check notifications every 30 seconds.
		while(running) {
			// Client connection test (ping pong).
			(ping = new Thread(new Runnable(){
				/* Sends a 'P' to the client and waits for response. */
				public void run() {
					try {
						out.writeUTF("P");
						in.readUTF();
					} catch(IOException e) {
						running = false;
						interrupt();
					}
				}
			}, "<"+this.user+"> Disconnection Handler")).start();
			// Try to refresh page
			try {
				studentHome = (HtmlPage) studentHome.refresh();
			} catch(Exception e) {
				System.out.println("<"+this.user+"> Could not refresh page. Continuing.");
			}
			// Get number of notifications.
			String notificationNum = studentHome.getElementById("badgeTotalCount").getTextContent().trim();
			// Prevents the user from getting spammed with the same notifications.
			sendData = (!notificationNum.equals("") && !notificationNum.equals(lastNum));
			zeroCount = ((notificationNum.equals("") && !lastNum.equals("0")) ? ((zeroCount < 5) ? zeroCount+1 : Integer.parseInt(lastNum = "0")) : 0);
			// Send number to client.
			if(sendData) {
				try {
					out.writeUTF("-n"+notificationNum);
					lastNum = notificationNum;
				} catch (IOException e) {
					break;
				}
			}

			try {
				Thread.sleep(30000);	// Wait 30 seconds
			} catch (InterruptedException e) {
				break;
			}

			if(ping.isAlive()) break;	// Disconnect client if 'P' not received after 30 seconds.
		}
		this.terminate();
		return;
	}

	/* Clean up when client disconnects. */
	private void terminate() {
		if(webClient != null) webClient.closeAllWindows();
		try {
			if(in != null) in.close();
			if(out != null) out.close();
			if(clientConn != null) clientConn.close();
			Server.removeClient(this);
			System.gc();
			System.out.println(this.user + " has disconnected. [" + new Timestamp(System.currentTimeMillis())+"]");
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
