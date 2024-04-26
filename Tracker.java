import java.io.*; // TODO: get rid of wildcard imports in future
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


class Tracker {
	boolean listening;
	ServerSocket ssocket;
	Map<String, User> registeredPeersInfo;
	Map<String, ContactInfo> activePeers;

	Tracker() {
		registeredPeersInfo = new HashMap<>();
		activePeers = new HashMap<>();
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
					System.err.println("Closing socket for " + csocket);
					csocket.close();
					output.close();
					input.close();
				}
			} catch (IOException e) {
				System.err.println("Could not close client socket.");
			}
		}

		/*
		 * Always get the output stream first, instead of the input stream. If
		 * you do, stdin fleezes and you'll be stuck forever.
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
		void echo() throws IOException {
			try {
				Message clientEcho;
				do {
					clientEcho = (Message) input.readObject();
					System.out.println(csocket + " ECHO: " + clientEcho.description);
					sendData(clientEcho); // ECHO Protocol
				} while (clientEcho.description != null && !clientEcho.description.equals("END"));
			} catch (ClassNotFoundException e) {
				System.err.println(csocket + "Received unknown object from client.");
			} finally {
				System.err.println(csocket + " Client quit.");
			}
		}

		void register() throws IOException {
			try {
				Message registration = (Message) input.readObject();
				String username = registration.username;
				String password = registration.password;
				Message response = new Message(MessageType.REGISTER);
				if ((response.status = !registeredPeersInfo.containsKey(username))) {
					User newUser = new User(username, password);
					registeredPeersInfo.put(username, newUser);
					System.out.println("Registered " + newUser.username);
				} else {
					response.description = "Username taken. Try another.";
				}
				sendData(response);
			} catch (ClassNotFoundException e) {
				System.err.println(csocket + "Received unknown object from client.");
			}
		}

		void login() throws IOException {
			try {
				Message registration = (Message) input.readObject();
				String username = registration.username;
				String password = registration.password;
				Message response = new Message(MessageType.REGISTER);
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
						System.out.println(info + ": active"); // calls info.toString();
						response.tokenID = tokenID;
					} else {
						response.description = "Invalid credentials.";
					}
				} else {
					response.description = "Invalid credentials.";
				}
				sendData(response);
			} catch (ClassNotFoundException e) {
				System.err.println(csocket + "Received unknown object from client.");
			}
		}

		/* TODO */
		void logout() throws IOException {
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
						case GENERIC:
							echo();
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
				e.printStackTrace();
			} finally {
				clientCleanup();
			}
		}
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
				System.out.println("Accepted connection: " + connection);
				new Thread(new TrackerThread(connection)).start();
			} while (listening);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			serverCleanup();
		}
	}
}
