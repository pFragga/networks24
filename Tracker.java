import java.io.*; // TODO: get rid of wildcard imports in future
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

class Tracker {
	boolean listening;
	ServerSocket ssocket;
	Map<String, User> registeredPeersInfo;
	Map<String, ContactInfo> activePeers;
	Set<String> allFilenames;
	Map<String, Set<String>> filenamesToTokenIDs;

	Tracker() {
		registeredPeersInfo = new HashMap<>();
		activePeers = new HashMap<>();
		allFilenames = new HashSet<>();
		filenamesToTokenIDs = new HashMap<>();
	}

	/*
	 * Nesting this class inside Tracker allows us to access shared data
	 * structures between threads.
	 *
	 * NOTE: some of these methods throw IOException. I left them like that, in
	 * order for them to be caught further down the call stack (usually in
	 * handleConnection).
	 */
	class TrackerThread implements Runnable {
		ObjectInputStream input;
		ObjectOutputStream output;
		Socket csocket;

		TrackerThread(Socket csocket) {
			this.csocket = csocket;
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
		 * you do, stdin freezes and you'll be stuck forever.
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

		/*
		 * I wrote this echo functionality for testing/debugging when I was
		 * rethinking things. It's not required by any other method.
		 */
		void echo() throws IOException, ClassNotFoundException {
			Message clientEcho;
			do {
				clientEcho = (Message) input.readObject();
				System.out.println(csocket + " ECHO: " + clientEcho.description);
				sendData(clientEcho); // ECHO Protocol
			} while (clientEcho.description != null && !clientEcho.description.equals("END"));
			System.out.println(csocket + " Client quit.");
		}

		void register() throws IOException, ClassNotFoundException {
			Message registration = (Message) input.readObject();
			String username = registration.username;
			String password = registration.password;
			Message response = new Message(MessageType.REGISTER);
			if ((response.status = !registeredPeersInfo.containsKey(username))) {
				User newUser = new User(username, password);
				registeredPeersInfo.put(username, newUser);
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
			if ((response.status = activePeers.containsKey(tokenID))) {
				for (String filename: information.sharedFilesNames) {
					updateAllFilenames(filename);
					updateFilenamesToTokenIDs(filename, tokenID);
				}
			} else {
				response.description = "Bad token ID.";
			}
			System.out.println("Updated data structures:\n" +
					"allFilenames = " + allFilenames +
					"\nfilenamesToTokenIDs = " + filenamesToTokenIDs);
			sendData(response);
		}

		void login() throws IOException {
			try {
				Message registration = (Message) input.readObject();
				String username = registration.username;
				String password = registration.password;
				Message response = new Message(MessageType.LOGIN);
				if ((response.status = registeredPeersInfo.containsKey(username))) {
					User user = registeredPeersInfo.get(username);
					if ((response.status = password.equals(user.password))) {
						Random rand = new Random();
						String tokenID = String.valueOf(rand.nextInt(10000));
						ContactInfo info = new ContactInfo(
								csocket.getInetAddress(),
								csocket.getPort(),
								tokenID,
								username);
						activePeers.put(tokenID, info);
						System.out.println("activePeers = " + activePeers);
						response.tokenID = tokenID;
					} else {
						response.description = "Invalid credentials.";
					}
				} else {
					response.description = "Invalid credentials.";
				}
				sendData(response);

				/* if peer could not login, no reason to inform */
				if (response.status)
					inform();
			} catch (ClassNotFoundException e) {
				System.err.println(csocket + "Received unknown object from client.");
			}
		}

		void logout() throws IOException, ClassNotFoundException {
			Message identification = (Message) input.readObject();
			String tokenID = identification.tokenID;
			Message response = new Message(MessageType.LOGOUT);
			if (response.status = activePeers.containsKey(tokenID)) {
				updateActivePeers(tokenID);
				updateFilenamesToTokenIDs(tokenID);
			}
			sendData(response);
		}

		void reply_list() throws IOException, ClassNotFoundException {
			Message response = new Message(MessageType.LIST);
			if (response.status = !allFilenames.isEmpty()) {
				response.description = "";
				for (String filename: allFilenames)
					response.description += filename + "\n";
			}
			sendData(response);
		}

		void reply_details() throws IOException, ClassNotFoundException {
			Message request = (Message) input.readObject();
			String filename = request.description;
			Message response = new Message(MessageType.DETAILS);
			Set<String> tokenIDs = filenamesToTokenIDs.get(filename);
			if (response.status = tokenIDs != null && !tokenIDs.isEmpty()) {
				ArrayList<ContactInfo> details = new ArrayList<>();
				for (String tokenID: tokenIDs) {
					details.add(activePeers.get(tokenID));
				}
				response.details = new ArrayList<>(details);
			} else {
				response.description = "No info about file '" + filename + "'";
			}
			sendData(response);
		}

		void handleConnection() throws IOException {
			try {
				while (!csocket.isClosed()) {
					Message request = (Message) input.readObject();
					switch (request.type) {
						case MessageType.REGISTER:
							register();
							break;
						case MessageType.LOGIN:
							login();
							break;
						case MessageType.LOGOUT:
							logout();
							break;
						case MessageType.GENERIC:
							echo();
							break;
						case MessageType.LIST:
							reply_list();
							break;
						case MessageType.DETAILS:
							reply_details();
							break;

						/* TODO: add more functionality here */

						default:
					}
				}
			} catch (ClassNotFoundException e) {
				System.err.println("Received unknown object from client.");
			}
		}

		@Override
		public void run() {
			try {
				getStreams();
				handleConnection();
			} catch (IOException e) {
				System.err.println(csocket + ": Terminated connection.");
			} finally {
				clientCleanup();
			}
		}
	}

	synchronized void updateAllFilenames(String filename) {
		allFilenames.add(filename);
	}

	synchronized void updateActivePeers(String tokenID) {
		activePeers.remove(tokenID);
	}

	synchronized void updateFilenamesToTokenIDs(String tokenID) {
		for (Set<String> tokenIDs : filenamesToTokenIDs.values()) {
			tokenIDs.remove(tokenID);
		}
	}

	synchronized void updateFilenamesToTokenIDs(String filename, String tokenID) {
		Set<String> tokenIDs;
		if (filenamesToTokenIDs.containsKey(filename)) {
			tokenIDs = filenamesToTokenIDs.get(filename);
		} else {
			tokenIDs = new HashSet<>();
		}
		tokenIDs.add(tokenID);
		filenamesToTokenIDs.put(filename, tokenIDs);
	}

	void serverCleanup() {
		try {
			if (ssocket != null && !ssocket.isClosed()) {
				System.err.println("Closing server socket.");
				ssocket.close();
				listening = false;
			}
		} catch (IOException e) {
			System.err.println("Could not close server socket.");
		}
	}

	void listen(int port) {
		try {
			listening = true;
			ssocket = new ServerSocket(port);
			do {
				System.out.println("Listening on port " + port + "...");
				Socket connection = ssocket.accept();
				System.out.println(connection + ": Accepted connection.");
				new Thread(new TrackerThread(connection)).start();
			} while (listening);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			serverCleanup();
		}
	}
}
