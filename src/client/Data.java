package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class Data {
	private static HashMap<String, String> valuesAndSettings = new HashMap<String, String>();
	private static File dataFile;

	/* Load client's login information or creates a new data file if there isn't one already. */
	public static void load() {
		File dir = null;
		// Creates new directory if there isn't one already.
		// (different for Windows and Linux/Mac)
		if(System.getProperty("os.name").startsWith("Windows")) {
			if(!(dir = new File(System.getProperty("user.home")+File.separator+"AppData"+File.separator+"Roaming"+File.separator+"bbn")).exists())
				dir.mkdir();
		} else {
			if(!(dir = new File(System.getProperty("user.home")+File.separator+".bbn")).exists())
				dir.mkdir();
		}
		// Gets data file.
		dataFile = new File(dir.toString()+File.separator+"data.dat");
		dir = null;
			
		valuesAndSettings.clear();
		if(!dataFile.exists()) {
			try {
				// If there's no data file, create a new one and prompt the client for their login information.
				// (Will be replaced with a GUI in the future to make it more user friendly).
				dataFile.createNewFile();
				Console console = System.console();
				Scanner scan = new Scanner(System.in);
				System.out.println("Enter Blackboard username: ");
				setValue("username", scan.nextLine());
				String pass = new String(console.readPassword("Enter Blackboard password: "));
				setValue("password", pass);
				scan.close();
				save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				// If there IS a data file, get the client's login information from it.
				BufferedReader currentSettings = new BufferedReader(new FileReader(dataFile.getAbsolutePath()));
				String line = "";
				while((line = currentSettings.readLine()) != null) {
					setValue(line.split("=")[0], line.split("=")[1]);
				}
				currentSettings.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/* Used to get a setting's value (i.e. the setting: "user" will return the client's username).
	 * (returns NULL if the input setting doesn't exist in the list.)*/
	public static String getValue(String setting) {
		return valuesAndSettings.get(setting);
	}

	/* Used to add a new setting to the list. */
	public static void setValue(String setting, String val) {
		valuesAndSettings.put(setting, val);
	}

	/* Saves all settings to the data file. */
	public static void save() {
		try {
			BufferedWriter settings = new BufferedWriter(new FileWriter(dataFile.getAbsolutePath()));
			for(Map.Entry<String, String> setting : valuesAndSettings.entrySet()) {
				settings.write(setting.getKey() + "=" + setting.getValue());
				settings.newLine();
			}
			settings.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
