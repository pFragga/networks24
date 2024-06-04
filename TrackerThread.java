import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

class TrackerThread implements Runnable {
	ObjectInputStream input;
	ObjectOutputStream output;
	Socket csocket;
	Tracker tracker;

	TrackerThread(Tracker tracker, Socket csocket) {
		this.csocket = csocket;
		this.tracker = tracker;
	}

	void clientCleanup() {
		try {
			if (csocket != null && !csocket.isClosed()) {
				csocket.close();
				output.close();
				input.close();
			}
		} catch (IOException e) {
			System.err.println(csocket + ": could not close client socket.");
		}
	}

	/*
	 * Always get the output stream first, instead of the input stream. If
	 * you do, input freezes and you'll be stuck forever.
	 * TODO: fix the computer world.
	 */
	void getStreams() throws IOException {
		output = new ObjectOutputStream(csocket.getOutputStream());
		output.flush();
		input = new ObjectInputStream(csocket.getInputStream());
	}

	void sendData(Message message) throws IOException {
		output.writeObject(message);
		output.flush();
	}

	void register() throws IOException, ClassNotFoundException {
		Message registration = (Message) input.readObject();
		String username = registration.username;
		String password = registration.password;
		Message response = new Message(MessageType.REGISTER);
		if (response.status = !tracker.registeredPeersInfo.containsKey(username)) {
			User newUser = new User(username, password);
			tracker.registeredPeersInfo.put(username, newUser);
			System.out.println("New User: " + newUser.username);
		} else {
			response.description = "Username taken. Try another.";
		}
		sendData(response);
	}

	void inform() throws IOException, ClassNotFoundException {
		Message information = (Message) input.readObject();
		String tokenID = information.tokenID;
		Message response = new Message(MessageType.INFORM);
		if (response.status = tracker.activePeers.containsKey(tokenID)) {
			for (String filename: information.sharedFilesNames)
				tracker.updateFilenamesToTokenIDs(filename, tokenID);
		} else {
			response.description = "Bad token ID.";
		}
		sendData(response);
	}

	void seederInform() throws IOException, ClassNotFoundException {
		Message seederInfo = (Message) input.readObject();
		String tokenID = seederInfo.tokenID;
		Message response = new Message(MessageType.SEEDER_INFORM);
		if (response.status = tracker.activePeers.containsKey(tokenID)) {
			for (Map.Entry<String, List<File>> entry : seederInfo.Pieces.entrySet()) {
				String filename = entry.getKey(); // Get the filename
				List<File> fileList = entry.getValue(); // Get the list of files

				// Iterate over the list of files for the current filename
				for (File file : fileList) {
					// Your logic here to update filenames to tokenIDs
					tracker.updateFilenamesToTokenIDs(filename, tokenID);
				}
			}
		} else {
			response.description = "Bad token ID.";
		}
		sendData(response);
	}

	void login() throws IOException, ClassNotFoundException {
		Message registration = (Message) input.readObject();
		String username = registration.username;
		String password = registration.password;
		int listeningPort = registration.listeningPort;
		Message response = new Message(MessageType.LOGIN);
		if (response.status = tracker.registeredPeersInfo.containsKey(username)) {
			User user = tracker.registeredPeersInfo.get(username);
			if ((response.status = password.equals(user.password))) {
				Random rand = new Random();
				String tokenID = String.valueOf(rand.nextInt(10000));
				ContactInfo info = new ContactInfo(
						csocket.getInetAddress(),
						listeningPort,
						tokenID,
						username);
				tracker.activatePeer(tokenID, info);
				response.tokenID = tokenID;
			} else {
				response.description = "Invalid credentials.";
			}
		} else {
			response.description = "Invalid credentials.";
		}
		sendData(response);

		/* if peer could not login, no reason to inform */
		if (response.status) {
			inform();
			tracker.postUpdateDataStructures();
		}
	}

	void logout() throws IOException, ClassNotFoundException {
		Message identification = (Message) input.readObject();
		String tokenID = identification.tokenID;
		Message response = new Message(MessageType.LOGOUT);
		if (response.status = tracker.activePeers.containsKey(tokenID)) {
			tracker.deactivatePeer(tokenID);
			tracker.updateFilenamesToTokenIDs(tokenID);
		}
		sendData(response);
		tracker.postUpdateDataStructures();
	}

	void reply_list() throws IOException, ClassNotFoundException {
		Message response = new Message(MessageType.LIST);
		if (response.status = !tracker.filenamesToTokenIDs.keySet().isEmpty()) {
			response.description = "";
			for (String filename: tracker.filenamesToTokenIDs.keySet())
				response.description += filename + "\n";
		}
		sendData(response);
	}

	void reply_details() throws IOException, ClassNotFoundException {
		Message request = (Message) input.readObject();
		String filename = request.description;
		Message response = new Message(MessageType.DETAILS);
		Set<String> tokenIDs = tracker.filenamesToTokenIDs.get(filename);
		if (response.status = tokenIDs != null && !tokenIDs.isEmpty()) {
			ArrayList<ContactInfo> details = new ArrayList<>();
			for (String tokenID: tokenIDs) {
				ContactInfo info = tracker.activePeers.get(tokenID);
				if (checkActive(info))
					details.add(tracker.activePeers.get(tokenID));
			}
			response.details = new ArrayList<>(details);
		} else {
			response.description = "No info about file '" + filename + "'";
		}
		sendData(response);
	}

	/*
	 * Checks if the peer provided in the contact info is listening for
	 * connections, by opening a new socket and new i/o streams.
	 */
	boolean checkActive(ContactInfo info) throws IOException, ClassNotFoundException {
		if (info == null)
			return false;
		System.out.print("Checking activity for " + info + "...");

		/*
		 * If we get IOException when opening the socket, that means the peer
		 * is definitely inactive and we can early return.
		 */
		Socket sock;
		try {
			sock = new Socket(info.ipAddr.getHostAddress(), info.port);
		} catch (IOException e) {
			System.err.println("NOT OK");
			return false;
		}

		ObjectInputStream in = new ObjectInputStream(
				sock.getInputStream());
		ObjectOutputStream out = new ObjectOutputStream(
				sock.getOutputStream());
		Message request = new Message(MessageType.ACTIVE);
		out.writeObject(request);
		out.flush();
		Message response = (Message) in.readObject();
		if (response.status) {
			System.out.println("OK");
		} else {
			System.out.println("NOT OK");
		}
		return response.status;
	}

	/*
	 * Different from the above method.
	 * Only replies to peers whether the tracker is up and running.
	 */
	void checkActive() throws IOException, ClassNotFoundException {
		Message response = new Message(MessageType.ACTIVE);
		response.status = tracker.listening;
		sendData(response);
	}

	void handleNotify() throws IOException, ClassNotFoundException {
		Message request = (Message) input.readObject();
		if (request.status) {
			tracker.activePeers.get(request.peer.tokenID).incrementCountDownloads();
		} else {
			tracker.activePeers.get(request.peer.tokenID).incrementCountFailures();
		}
		tracker.postUpdateDataStructures();
	}

	void handleConnection() throws IOException {
		try {
			while (!csocket.isClosed()) {
				Message request = (Message) input.readObject();
				switch (request.type) {
					case REGISTER:
						register();
						break;
					case LOGIN:
						login();
						break;
					case LOGOUT:
						logout();
						break;
					case LIST:
						reply_list();
						break;
					case DETAILS:
						reply_details();
						break;
					case ACTIVE:
						checkActive();
						break;
					case NOTIFY:
						handleNotify();
						break;
					case INFORM:
						inform();
						break;
					case SEEDER_INFORM:
						seederInform();
						break;
					default:
						break;
				}
			}
		} catch (ClassNotFoundException e) {
			System.err.println(csocket + ": received unknown object.");
			e.printStackTrace();
		} catch (EOFException e) {
			System.err.println(csocket + ": quit.");
		}
	}

	@Override
	public void run() {
		try {
			getStreams();
			handleConnection();
		} catch (IOException e) {
			System.err.println(csocket + ": Terminated connection.");
			e.printStackTrace();
		} finally {
			clientCleanup();
		}
	}
}
