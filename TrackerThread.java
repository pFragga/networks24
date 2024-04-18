import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Thread;
import java.net.Socket;
import java.util.ArrayList;

public class TrackerThread implements ITracker, Runnable {
	//List<User> registeredPeersInfo = new ArrayList<>();
	//List<User> activePeersInfo = new ArrayList<>();
	//List<String> allFilenames;
	//Map<String, Integer> filenamesToTokenIDs = null;
	private static int id = 1;
	private Socket clientSocket;

	public TrackerThread(Socket clientSocket) {
		this.id++;
		this.clientSocket = clientSocket;
	}

	public static void greetUser(ObjectOutputStream out, Message message) {
		try {
			out.writeObject("Welcome, " + message.username + "!!");
			out.flush();
		} catch (Exception e) {
			System.err.println("Could not welcome user. :(");
		}
	}

	@Override
	public void register() {
	}

	@Override
	public void login() {
	}

	@Override
	public void logout() {
	}

	@Override
	public void respondToNotify() {
	}

	@Override
	public void reply_list() {
	}

	@Override
	public void reply_details() {
	}

	@Override
	public void run() {
		System.out.println("Thread '" + this.getName() + "' accepted a new connection: "
				+ clientSocket.toString());
		try {
			// get Input and Output streams
			ObjectOutputStream out = new
				ObjectOutputStream(clientSocket.getOutputStream());
			ObjectInputStream in = new
				ObjectInputStream(clientSocket.getInputStream());

			Message message = (Message) in.readObject();
			switch (message.msg_type) {
				case 1:
					greetUser(out, message);
					break;
				default:
			}
			out.flush(); // ensure flushed output before closing
			clientSocket.close();
		} catch (ClassNotFoundException e) {
			System.err.println("Invalid message. Aborting...");
		} catch (IOException e) {
			System.err.println("Connection error. Aborting...");
		}
	}
}
