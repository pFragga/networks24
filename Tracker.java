import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Tracker {
	/* active users are always a subset of registered users */
	private ArrayList<User> activeUsers = new ArrayList<>();
	private ArrayList<User> registeredUsers = new ArrayList<>();

	private void register() {
	}

	private void login() {
	}

	private void logout() {
	}

	public static void main(String[] args) {
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
