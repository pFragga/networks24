import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Thread;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerThread implements ITracker, Runnable {
	//List<User> registeredPeersInfo = new ArrayList<>();
	private static Map<Integer, User> activePeers;
	private static List<String> allFilenames;
	private static Map<String, List<Integer>> filenamesToTokenIDs;
	private static int id = 1;
	private Socket clientSocket;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private ConcurrentHashMap<String, Integer> countDownloads = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, Integer> countFailures = new ConcurrentHashMap<>();
	public TrackerThread(Socket clientSocket) {
		id++;
		this.clientSocket = clientSocket;
		allFilenames = new ArrayList<>();
		filenamesToTokenIDs = new HashMap<>();
		activePeers = new HashMap<>();
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
		message.status = allFilenames == null;
		try {
			out.writeObject(message);
			out.flush();
		} catch (IOException e) {
			System.err.println("Could not send requested list. Aborting...");
		}
	}

	/**
	 * Show info for all users that provide the file with given name.
	 *
	 * First, locate which tokenID(s) correspond to the provided filename.
	 * Then, for each tokenID, if the user holding it is active, we send the
	 * needed details to the client.
	 */
	@Override
	public void reply_details(String filename) {
		Message message = new Message(11);
		List<Integer> tokenIDs = filenamesToTokenIDs.get(filename);
		if ((message.status = tokenIDs != null && !tokenIDs.isEmpty())) {
			message.providersForReqFile = new ArrayList<>();
			for (Integer tokenID : tokenIDs) {
				User currUser = activePeers.get(tokenID);
				if (checkActive(currUser.getAddr(), currUser.getPort()))
					/*
					 * FIXME
					 * Don't send the entire User class, the passwords are also
					 * stored there!!!
					 */
					message.providersForReqFile.add(currUser);
			}
		}
		try {
			out.writeObject(message);
			out.flush();
		} catch (IOException e) {
			System.err.println("Could not send details. Aborting...");
		}
	}

	@Override
	public boolean checkActive(String ipAddr, int port) {
		try {
			Socket newSocket = new Socket(ipAddr, port);
			ObjectOutputStream newOut = new ObjectOutputStream(newSocket.getOutputStream());
			ObjectInputStream newIn = new ObjectInputStream(newSocket.getInputStream());
			Message message = new Message(12);
			newOut.writeObject(message);
			newOut.flush();
			Message reply = (Message) newIn.readObject();
			if (reply.status)
				return reply.peer_active;
		} catch (ClassNotFoundException e) {
			System.err.println("Invalid message. Aborting...");
		} catch (IOException e) {
			System.err.println("Connection error. Aborting...");
			return false;
		}
		return false;
	}

	private void closeClientSocket() {
		try {
			if (clientSocket != null && !clientSocket.isClosed())
				clientSocket.close();
		} catch (IOException e) {
			System.err.println("Connection error. Aborting...");
		}
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
					respondToNotify(message.username, message.status);
					break;
				case 10:
					reply_list();
					break;
				case 11:
					reply_details(message.requestedFileName);
					break;
				default:
			}
			out.flush(); // ensure flushed output before closing
			closeClientSocket();
		} catch (ClassNotFoundException e) {
			System.err.println("Invalid message. Aborting...");
			closeClientSocket();
		} catch (IOException e) {
			System.err.println("Connection error. Aborting...");
			closeClientSocket();
		}
	}
}
