package client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

public class NotificationGUI extends JWindow {
	private static final long serialVersionUID = 1L;
	private final int WIDTH = 300;
	private final int HEIGHT = 90;

	public NotificationGUI(String data) {
		// Get screen dimensions.
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int screenWidth = gd.getDisplayMode().getWidth();
		int screenHeight = gd.getDisplayMode().getHeight();
		
		// Setup and add the main panel to the window.
		GUIPanel guiPanel = new GUIPanel(data);
		guiPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		this.add(guiPanel);

		this.pack();
		this.setAlwaysOnTop(true);
		this.setLocation(screenWidth - WIDTH, screenHeight - HEIGHT);
		this.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				// Shows close button when cursor enters the window.
				guiPanel.setCloseButtonVisible(true);
			}

			public void mouseExited(MouseEvent e) {
				// Hides close button when cursor leaves the window.
				guiPanel.setCloseButtonVisible(false);
			}

			public void mouseClicked(MouseEvent e) {
				// Closes the notification GUI when the close button is clicked.
				if(e.getX() >= (Client.isWindows ? guiPanel.CLOSE_BUTTON_X : 0) 
				&& e.getX() <= (guiPanel.CLOSE_BUTTON_X+guiPanel.CLOSE_BUTTON_SIZE) 
				&& e.getY() >= 0 
				&& e.getY() <= (guiPanel.CLOSE_BUTTON_Y+guiPanel.CLOSE_BUTTON_SIZE)) {
					dispose();
				}
			}
		});
		this.setVisible(true);
		
		// Closes the notification GUI if it receives a unknown identifier (i.e. not '-n' or '-m')
		if(!guiPanel.isEnabled())
			this.dispose();
	}
	
	/* Main part of the notification GUI - where all the graphics are drawn. */
	private class GUIPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private final int TITLE_Y = 20;
		private final int TITLE_FONT_SIZE = 20;

		private final int MESSAGE_Y = 60;
		private final int MESSAGE_FONT_SIZE = 15;

		private final int CLOSE_BUTTON_X;
		private final int CLOSE_BUTTON_Y = 7;
		private final int CLOSE_BUTTON_SIZE = 13;

		private String noteNum, noteMsg;
		private int noteNumXPos, xModifier;
		private Font titleFont, noteNumFont, noteStringFont;

		private boolean closeButtonVisable = false;

		public GUIPanel(String data) {
			// Puts close button at top right for Windows and top left for Linux/Mac.
			CLOSE_BUTTON_X = (Client.isWindows ? this.getWidth()-CLOSE_BUTTON_SIZE : 7);
			
			// Processes data from server before displaying it.
			noteNum = "";
			String msg = data.substring(2);
			switch(data.substring(0, 2)) {
			case "-n":	// Notification.
				noteNum = msg;
				noteMsg = " new notification" + ((noteNum.equals("1")) ? "" : "s");
				break;
			case "-m":	// Generic message from the server. (Not yet implemented).
				noteMsg = msg;
				break;
			default:	// Unknown identifier from server (i.e. not '-n' or '-m').
				System.out.println("Unknown identifier: "+msg);
				this.setEnabled(false);
				noteMsg = "";
				break;
			}

			titleFont = new Font("sans-serif", Font.PLAIN, TITLE_FONT_SIZE);
			noteNumFont = new Font("sans-serif", Font.BOLD, MESSAGE_FONT_SIZE);
			noteStringFont = new Font("sans-serif", Font.PLAIN, MESSAGE_FONT_SIZE);
		}

		/* Adds all the graphics to the panel. */
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			// Background and outline
			g.setColor(Color.decode("0x2d4b77"));
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			g.setColor(Color.decode("0x182840"));
			g.drawRect(0, 0, this.getWidth(), this.getHeight());

			// Title
			g.setColor(Color.white);
			g.setFont(titleFont);
			drawCenteredString(g, "Blackboard", (this.getWidth()/2), TITLE_Y);

			// Notification message
			noteNumXPos = (this.getWidth()/2) - (g.getFontMetrics(noteStringFont).stringWidth(noteMsg)/2) - g.getFontMetrics(noteNumFont).stringWidth(noteNum);
			xModifier = g.getFontMetrics(noteNumFont).stringWidth(noteNum)/2;
			g.setFont(noteStringFont);
			drawCenteredString(g, noteMsg, (this.getWidth()/2)+xModifier, MESSAGE_Y);
			g.setFont(noteNumFont);
			drawCenteredString(g, noteNum, noteNumXPos+(xModifier*2), MESSAGE_Y);

			// Close button (only visible when cursor is in JWindow)
			if(this.closeButtonVisable) {
				g.setColor(Color.decode("0xe81c2b"));
				if(Client.isWindows) g.fillRect(CLOSE_BUTTON_X, CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
				else g.fillOval(CLOSE_BUTTON_X, CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
				g.setColor(Color.decode("0xe81c2b").darker().darker().darker());
				if(Client.isWindows) g.drawRect(CLOSE_BUTTON_X, CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
				else g.drawOval(CLOSE_BUTTON_X, CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
			}
		}

		/* Used to draw centered strings on the panel 
		 * (Java's Graphics class doesn't have a 'drawCenteredString' method). */
		private void drawCenteredString(Graphics g, String s, int x, int y) {
			g.drawString(s, x - (g.getFontMetrics().stringWidth(s)/2), y + (g.getFontMetrics().getHeight()/2));
		}

		/* Shows/hides the close button. */
		private void setCloseButtonVisible(boolean b) {
			this.closeButtonVisable = b;
			this.repaint();
		}
	}
}
