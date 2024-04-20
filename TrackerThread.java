import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Thread;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerThread implements ITracker, Runnable {
	//List<User> registeredPeersInfo = new ArrayList<>();
	//List<User> activePeersInfo = new ArrayList<>();
	List<String> allFilenames;
	//Map<String, Integer> filenamesToTokenIDs = null;
	private static int id = 1;
	private Socket clientSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private ConcurrentHashMap<String, Integer> countDownloads = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Integer> countFailures = new ConcurrentHashMap<>();
	public TrackerThread(Socket clientSocket) {
		this.id++;
		this.clientSocket = clientSocket;
		this.allFilenames = new ArrayList<>();
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
	public synchronized void respondToNotify(String userName, boolean isSuccess) {
		if (isSuccess) {
			// Update count_downloads
			countDownloads.merge(userName, 1, Integer::sum);
			// Inform about successful download
			System.out.println("Successfully downloaded by: " + userName);
		} else {
			// Update count_failures
			countFailures.merge(userName, 1, Integer::sum);
			// Inform about download failure
			System.out.println("Download failed for: " + userName);
		}
	}

	@Override
	public void reply_list() {
		Message message = new Message(10);
		message.availableFiles = new ArrayList<>(allFilenames);
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
