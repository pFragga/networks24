import java.io.*;
import java.net.*;

class Tracker {
	/*
	 * Create the file that holds our users (initially empty).
	 */
	public static void init_db() throws IOException {
		BufferedWriter writer =
			new BufferedWriter(new FileWriter("users.db"));
		writer.write(100);
		writer.close();
	}


	public static void main(String[] args) {
		try {
			init_db(); // TODO
		} catch (IOException e) {
			System.err.println("Could not write database file.");
			System.exit(1);
		}

		int port = 9090;
		boolean listening = true;
		try {
			ServerSocket ssocket = new ServerSocket(port);
			while (listening) {
				Socket csocket = ssocket.accept();
				new TrackerThread(csocket).start();
			}
		} catch (IOException e) {
			System.err.println("Connection lost.");
			System.exit(1);
		}
	}
}
