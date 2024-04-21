import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Thread;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TrackerThread implements ITracker, Runnable {
	//List<User> registeredPeersInfo = new ArrayList<>();
	//List<User> activePeersInfo = new ArrayList<>();
	//Map<String, Integer> filenamesToTokenIDs = null;
	private static List<String> allFilenames;
	private static int id = 1;
	private Socket clientSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	public TrackerThread(Socket clientSocket) {
		id++;
		this.clientSocket = clientSocket;
		allFilenames = new ArrayList<>();
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
		Message message = new Message(10);
		message.availableFiles = new ArrayList<>(allFilenames);
		message.status = allFilenames == null;
		try {
			out.writeObject(message);
			out.flush();
		} catch (IOException e) {
			System.err.println("Could not send requested list. Aborting...");
		}
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
				case 2:
					login();
					break;
				case 3:
					logout();
					break;
				case 4:
					respondToNotify();
					break;
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
