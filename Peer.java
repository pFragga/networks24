import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Math;
import java.lang.Thread;
import java.net.ServerSocket;
import java.net.Socket;
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
	int listeningPort;
	ServerSocket serverSocket;
	List<ContactInfo> knownPeers;

	/* tracker info */
	String trackerHost;
	int trackerPort;

	Peer(String trackerHost, int trackerPort, String sharedDir) {
		this.trackerHost = trackerHost;
		this.trackerPort = trackerPort;
		this.sharedFiles = new ArrayList<>();
		this.sharedDir = sharedDir;
		this.knownPeers = new ArrayList<>();

		/* random port between 10000 and 25000 */
		this.listeningPort = (int) (Math.random() * (25000 - 10000) + 10000);
	}

	void getHelp() {
		System.out.println(
				"[c]\tconnect to tracker\n" +
				"[d]\tdisconnect from tracker\n\n" +
				"[e]\techo server (useful for debugging)\n\n" +
				"[r]\tregister (requires connection)\n" +
				"[l]\tlogin (requires connection)\n" +
				"[L]\tlogout (requires connection)\n\n" +
				"[ls]\tlist tracker's known files\n" +
				"[Q]\tquery details about given file\n" +
				"[ch]\tcheck if user is active\n\n" +
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
				input.close();
				output.close();
				connected = false;
				System.out.println("Disconnected.");
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
		if (!connected)
			return;
		output.writeObject(message);
		output.flush();
	}

	void register() throws IOException, ClassNotFoundException {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return;
		}

		if (registered) {
			System.out.println("You need to logout first.");
			return;
		}

		sendData(new Message(MessageType.REGISTER));

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
			System.out.println("Registration failed. Reason: " + response.description);
		}
	}

	void inform() throws IOException, ClassNotFoundException {
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
	}

	void login() throws IOException, ClassNotFoundException {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return;
		}

		if (tokenID != null && !tokenID.isEmpty()) {
			System.out.println("You are already logged in.");
			return;
		}

		sendData(new Message(MessageType.LOGIN));

		Message credentials = new Message(MessageType.LOGIN);
		System.out.print("Enter your username: ");
		credentials.username = stdin.nextLine();
		System.out.print("Enter your password: ");
		credentials.password = stdin.nextLine(); // TODO: hide password
		credentials.listeningPort = listeningPort;
		sendData(credentials);
		Message response = (Message) input.readObject();
		if (response.status) {
			tokenID = response.tokenID;
			System.out.println("Login successful. TOKENID: " + tokenID);
			inform();
		} else {
			System.out.println("Login failed. Reason: " + response.description);
		}
	}

	boolean checkActive() throws IOException, ClassNotFoundException {
		Message request = new Message(MessageType.ACTIVE);
		System.out.print("Ping tracker? [y/N] ");
		String reply = stdin.nextLine();
		if (reply.equals("y") || reply.equals("Y"))
			return checkActiveTracker(request);
		return checkActivePeer(request);
	}

	boolean checkActiveTracker(Message request) throws IOException, ClassNotFoundException {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return false;
		}
		System.out.print("Tracker status... ");
		sendData(request);
		Message response = (Message) input.readObject();
		if (response.status) {
			System.out.println("active.");
		} else {
			System.out.println("inactive.");
		}
		return response.status;
	}

	boolean checkActivePeer(Message request) throws IOException, ClassNotFoundException {
		/* show known peers and select one */
		if (knownPeers == null || knownPeers.isEmpty()) {
			System.out.println("No known peers yet.");
			return false;
		}
		System.out.println("Known Peers\n===========");
		int i = 0;
		for (i = 0; i < knownPeers.size(); ++i) {
			System.out.println(i + ") " + knownPeers.get(i));
		}
		System.out.print("Who do you want to ping? [number] ");
		i = Integer.parseInt(stdin.nextLine());

		/* sanity check on user input */
		if (i < 0 || i > 100)
			return false;

		/* establish connection to selected peer */
		ContactInfo info = knownPeers.get(i);
		System.out.print(info + " status... ");
		Socket tmpSock = new Socket(info.getIP(), info.port);
		ObjectInputStream in = new ObjectInputStream(tmpSock.getInputStream());
		ObjectOutputStream out = new ObjectOutputStream(tmpSock.getOutputStream());
		out.writeObject(request);
		out.flush();
		Message response = (Message) in.readObject();
		if (response.status) {
			System.out.println("active.");
		} else {
			System.out.println("inactive.");
		}
		
		/* cleanup */
		tmpSock.close();
		in.close();
		out.close();

		return response.status;
	}

	void logout() throws IOException, ClassNotFoundException {
		if (!connected) {
			System.out.println("You are already not connected.");
			return;
		}

		if (tokenID == null || tokenID.isEmpty()) {
			System.out.println("You have not yet logged in.");
			return;
		}

		sendData(new Message(MessageType.LOGOUT));

		Message identification = new Message(MessageType.LOGOUT);
		identification.tokenID = tokenID;
		sendData(identification);
		Message response = (Message) input.readObject();
		if (response.status) {
			tokenID = "";
			System.out.println("Logout successful.");
		} else {
			System.out.print("Logout failed. Reason: " + response.description);
		}
	}

	void list() throws IOException, ClassNotFoundException {
		if (!connected) {
			System.out.println("You are already not connected.");
			return;
		}

		if (tokenID == null || tokenID.isEmpty()) {
			System.out.println("You have not yet logged in.");
			return;
		}

		sendData(new Message(MessageType.LIST));

		Message response = (Message) input.readObject();
		if (response.status) {
			System.out.println("Available files\n===============\n" +
					response.description);
		} else {
			System.out.println("No available files yet.");
		}
	}

	void details() throws IOException, ClassNotFoundException {
		list();

		sendData(new Message(MessageType.DETAILS));

		System.out.print("Details for which file? [filename] ");
		String filename = stdin.nextLine();
		Message request = new Message(MessageType.DETAILS);
		request.description = filename;
		sendData(request);
		Message response = (Message) input.readObject();
		if (response.status) {
			System.out.println(filename + "\n==========");
			for (ContactInfo info: response.details) {
				knownPeers.add(info);
				System.out.println(info);
			}
		} else {
			System.out.println(response.description);
		}
	}

	void echo() throws IOException, ClassNotFoundException {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return;
		}

		sendData(new Message()); /* GENERIC */

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

	void startServer() {
		Thread receive = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					serverSocket = new ServerSocket(listeningPort, 20); // Use the listeningPort defined in your Peer class
					while (running) {
						Socket socket = serverSocket.accept();

						// Handle each request in a new thread
						new Thread(new Runnable() {
							Socket socket;
							ObjectOutputStream out;
							ObjectInputStream in;

							public Runnable init(Socket socket) {
								this.socket = socket;
								return this;
							}

							boolean handleSimpleDownload(Message request) throws IOException {
								String filename = request.description;
								File f = new File(sharedDir, filename);
								long flength = f.length();

								/*
								 * check if we can actually send the file, then
								 * inform the requesting peer
								 */
								Message response;
								if (!f.exists() || flength > Integer.MAX_VALUE) {
									response = new Message(false, MessageType.DOWNLOAD);
									response.description = "Could not send '" + filename + "' to " + socket;
									System.out.println(response.description);
									out.writeObject(response);
									out.flush();
									return false;
								}
								response = new Message(MessageType.DOWNLOAD);
								System.out.println(socket + ": requested " + filename);
								out.writeObject(response);
								out.flush();

								byte[] bytes = new byte[(int) flength];
								BufferedInputStream bis = new
									BufferedInputStream(new FileInputStream(f));
								bis.read(bytes, 0, bytes.length);
								out.write(bytes, 0, bytes.length);
								out.flush();
								bis.close();
								System.out.println("Sent '" + filename + "' to " + socket);
								return true; /* TODO */
							}

							@Override
							public void run() {
								try {
									out = new ObjectOutputStream(socket.getOutputStream());
									in = new ObjectInputStream(socket.getInputStream());
									// Read the request type
									Message request = (Message) in.readObject();
									switch (request.type) {
									case ACTIVE:
										Message response = new Message(
												running, /* status */
												MessageType.ACTIVE
												);
										out.writeObject(response);
										out.flush();
										break;
									case DOWNLOAD:
										handleSimpleDownload(request);
										break;
									default:
										System.err.println("received unknown message type");
										break;
									}

									// Close streams and socket
									out.close();
									in.close();
									socket.close();
								} catch (IOException | ClassNotFoundException e) {
									e.printStackTrace();
								}
							}
						}.init(socket)).start();
					}
				} catch (IOException e) {
					System.out.println("Server stopped receiving requests");
				}
			}
		});
		receive.start();
	}

	void begin() {
		running = true;
		updateSharedFiles();
		connect(trackerHost, trackerPort); // attempt connection on startup
		startServer(); // Start the server to handle incoming requests
		while (running) {
			System.out.print("(h for help)> ");
			String letter = stdin.nextLine();
			try {
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
					case "ls":
						list();
						break;
					case "Q": // Q, as in Query
						details();
						break;
					case "h":
						getHelp();
						break;
					case "q":
						if (tokenID != null && !tokenID.isEmpty())
							logout();
						if (connected)
							disconnect();
						if (!serverSocket.isClosed())
							serverSocket.close();
						running = false;
						break;
					case "ch":
						checkActive();
						break;
					default:
						break;
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.err.println("Received unknown object from server.");
			} catch (IOException e) {
				System.err.println("Could not send message.");
				e.printStackTrace();
			}
		}

		stdin.close();
	}

}
