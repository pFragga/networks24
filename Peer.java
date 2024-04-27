import java.io.*; // TODO: get rid of wildcard imports in future
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class Peer {
	/* use this to read all input from the user */
	static final Scanner stdin = new Scanner(System.in);

	boolean connected;
	boolean running;
	boolean registered;
	String tokenID; // this is the equivalent of having a 'loggedin' boolean
	ObjectInputStream input;
	ObjectOutputStream output;
	Socket csocket;
	List<File> sharedFiles;
	String sharedDir;

	/* tracker info */
	String trackerHost;
	int trackerPort;

	Peer(String trackerHost, int trackerPort, String sharedDir) {
		this.trackerHost = trackerHost;
		this.trackerPort = trackerPort;
		this.sharedFiles = new ArrayList<>();
		this.sharedDir = sharedDir;
	}

	void getHelp() {
		System.out.println(
				"[c]\tconnect to tracker\n" +
				"[d]\tdisconnect from tracker\n\n" +
				"[e]\techo server (useful for debugging)\n\n" +
				"[r]\tregister (requires connection)\n" +
				"[l]\tlogin (requires connection)\n" +
				"[L]\tlogout (requires connection)\n\n" +
				"[h]\tshow help info\n" +
				"[q]\tquit (implies logout and disconnect)");
	}

	void connect(String host, int port) {
		try {
			if (!connected) {
				csocket = new Socket(host, port);
				System.out.println("Connected to " + host + ":" + port);
				connected = true;
				getStreams();
			} else {
				System.out.println("Already connected to " + host + ":" + port);
			}
		} catch (IOException e) {
			System.err.println("Could not connect to " + host + ":" + port);
		}
	}

	void disconnect() {
		try {
			if (connected && !csocket.isClosed()) {
				csocket.close();
				System.out.println("Disconnected.");
				input.close();
				output.close();
				connected = false;
			} else {
				System.out.println("You are already disconnected.");
			}
		} catch (IOException e) {
			System.err.println("Could not disconnect");
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

	void register() {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return;
		}

		if (registered) {
			System.out.println("You need to logout first.");
			return;
		}

		requestOperation(new Message(MessageType.REGISTER));
		try {
			Message registration = new Message(MessageType.REGISTER);
			System.out.print("Enter your username: ");
			registration.username = stdin.nextLine();
			System.out.print("Enter your password: ");
			registration.password = stdin.nextLine(); // TODO: hide password
			sendData(registration);
			Message response = (Message) input.readObject();
			if (response.status) {
				registered = true;
				System.out.println("Registration successful.");
			} else {
				System.out.println("Registration failed. Reason: "
						+ response.description);
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Received unknown object from server.");
		} catch (IOException e) {
			System.err.println("Could not send message.");
		}
	}

	void inform() {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return;
		}

		if (tokenID == null || tokenID.isEmpty()) {
			System.out.println("You are already logged in.");
			return;
		}

		/* in case a new file has been added in the meantime... */
		updateSharedFiles();

		try {
			Message information = new Message(MessageType.INFORM);
			ArrayList<String> sharedFilesNames = new ArrayList<>();
			for (File f: sharedFiles) {
				sharedFilesNames.add(f.getName());
			}
			information.sharedFilesNames = sharedFilesNames;
			information.tokenID = tokenID;
			sendData(information);
			Message response = (Message) input.readObject();
			if (response.status) {
				System.out.println("Successfully informed tracker about shared files.");
			} else {
				System.out.println("Failed to inform tracker about shared files."
						+ "Reason: " + response.description);
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Received unknown object from server.");
		} catch (IOException e) {
			System.err.println("Could not send message.");
		}
	}

	void login() {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return;
		}

		if (tokenID != null && !tokenID.isEmpty()) {
			System.out.println("You are already logged in.");
			return;
		}

		requestOperation(new Message(MessageType.LOGIN));
		try {
			Message credentials = new Message(MessageType.LOGIN);
			System.out.print("Enter your username: ");
			credentials.username = stdin.nextLine();
			System.out.print("Enter your password: ");
			credentials.password = stdin.nextLine(); // TODO: hide password
			sendData(credentials);
			Message response = (Message) input.readObject();
			if (response.status) {
				tokenID = response.tokenID;
				System.out.println("Login successful. TOKENID: " + tokenID);
				inform();
			} else {
				System.out.println("Login failed. Reason: "
						+ response.description);
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Received unknown object from server.");
		} catch (IOException e) {
			System.err.println("Could not send message.");
		}
	}

	void logout() {
		if (!connected) {
			System.out.println("You are already not connected.");
			return;
		}

		if (tokenID == null || tokenID.isEmpty()) {
			System.out.println("You have not yet logged in.");
			return;
		}

		requestOperation(new Message(MessageType.LOGOUT));
		try {
			Message identification = new Message(MessageType.LOGOUT);
			identification.tokenID = tokenID;
			sendData(identification);
			Message response = (Message) input.readObject();
			if (response.status) {
				connected = false;
				tokenID = "";
				System.out.println("Logout successful.");
			} else {
				System.out.print("Logout failed. Reason: "
						+ response.description);
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Received unknown object from server.");
		} catch (IOException e) {
			System.err.println("Could not send message.");
		}
	}

	void echo() {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return;
		}

		requestOperation(new Message()); // GENERIC
		try {
			String echoStr;
			do {
				System.out.print("CLIENT ECHO: ");
				echoStr = stdin.nextLine();
				Message clientEcho = new Message();
				clientEcho.description = echoStr;
				sendData(clientEcho);
				Message serverEcho = (Message) input.readObject();
				System.out.println("SERVER ECHO: " + serverEcho.description);
			} while (echoStr != null && !echoStr.equals("END"));
		} catch (ClassNotFoundException e) {
			System.err.println("Received unknown object from server.");
		} catch (IOException e) {
			System.err.println("Could not send message.");
		}
	}

	/*
	 * Establishes a "handshake" with the Tracker. Based on the request's
	 * MessageType, the Tracker knows which method to call.
	 */
	void requestOperation(Message request) {
		if (!connected) {
			return;
		}

		try {
			sendData(request);
		} catch (IOException e) {
			System.err.println("Could not send request.");
		}
	}

	/*
	 * Reading 'fileDownloadList.txt' is really unnecessary, since we could
	 * just get the listing of sharedDir instead, but...
	 *
	 * May have to send another email to <pittaras@aueb.gr>
	 */
	void updateSharedFiles() {
		try (
			BufferedReader reader = new BufferedReader(new FileReader("fileDownloadList.txt"));
			) {
			System.out.print("Updating shared files... ");
			String filename;
			while ((filename = reader.readLine()) != null) {
				File currFile = new File(sharedDir, filename);
				if (currFile.exists() && !sharedFiles.contains(currFile))
					sharedFiles.add(currFile);
			}
			System.out.println("OK.");
		} catch (IOException e) {
			System.err.println("Could not read fileDownloadList.txt");
		}
	}

	void begin() {
		running = true;
		updateSharedFiles();
		connect(trackerHost, trackerPort); // attempt connection on startup
		while (running) {
			System.out.print("(h for help)> ");
			String letter = stdin.nextLine();
			switch (letter) {
				case "c":
					connect(trackerHost, trackerPort);
					break;
				case "d":
					disconnect();
					break;
				case "e":
					echo();
					break;
				case "r":
					register();
					break;
				case "l":
					login();
					break;
				case "L":
					logout();
					break;
				case "h":
					getHelp();
					break;
				case "q":
					if (tokenID != null && !tokenID.isEmpty())
						logout();
					if (connected)
						disconnect();
					running = false;
					break;
				default:
			}
		}
	}
}
