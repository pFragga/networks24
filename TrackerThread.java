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
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public TrackerThread(Socket clientSocket) {
		this.id++;
		this.clientSocket = clientSocket;
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
		System.out.println("Accepted new connection: " + clientSocket);
		try {
			// get Input and Output streams
			out = new ObjectOutputStream(clientSocket.getOutputStream());
			in = new ObjectInputStream(clientSocket.getInputStream());

			Message message = (Message) in.readObject();
			switch (message.msg_type) {
				case 1:
					register();
					break;

				/* TODO: add more functions here */

				case 10:
					reply_list();
					break;
				case 11:
					reply_details();
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
