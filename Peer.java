import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Math;
import java.lang.Thread;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
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
	String lastRequestedFilename;

	/* tracker info */
	String trackerHost;
	int trackerPort;

	Peer(String trackerHost, int trackerPort, String sharedDir) {
		this.trackerHost = trackerHost;
		this.trackerPort = trackerPort;
		this.sharedFiles = new ArrayList<>();
		this.sharedDir = sharedDir;

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
				"[D]\tdownload given file\n" +
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

	/*
	 * Show a menu of known peers and prompt the user to select one. Return
	 * the corresponding ContactInfo entry in peers.
	 */
	ContactInfo selectPeer(List<ContactInfo> peers, String prompt) {
		if (peers == null || peers.isEmpty())
			return null;

		System.out.println("Known Peers\n===========");
		for (int i = 0; i < peers.size(); ++i) {
			System.out.println(i + ") " + peers.get(i));
		}
		System.out.print(prompt);
		int choice = Integer.parseInt(stdin.nextLine());

		/* sanity check */
		if (choice < 0 || choice > 100) {
			System.out.println("Invalid selection");
			return null;
		}

		return peers.get(choice);
	}

	boolean checkActive() throws IOException, ClassNotFoundException {
		System.out.print("Ping tracker? [y/N] ");
		String reply = stdin.nextLine();
		if (reply.equals("y") || reply.equals("Y"))
			return checkActiveTracker();

		List<ContactInfo> peers = details();
		ContactInfo selected = selectPeer(peers, "Who do you want to ping? ");
		if (peers == null || selected == null)
			return false;
		return checkActivePeer(selected);
	}

	boolean checkActiveTracker() throws IOException, ClassNotFoundException {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return false;
		}
		System.out.print("Tracker status... ");
		sendData(new Message(MessageType.ACTIVE));
		Message response = (Message) input.readObject();
		if (response.status) {
			System.out.println("active.");
		} else {
			System.out.println("inactive.");
		}
		return response.status;
	}

	boolean checkActivePeer(ContactInfo peer) throws IOException, ClassNotFoundException {
		Message request = new Message(MessageType.ACTIVE);

		/* establish connection to selected peer */
		System.out.print("Checking status for " + peer.username + "...");
		Socket tmpSock = new Socket(peer.getIP(), peer.port);
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

	List<ContactInfo> details() throws IOException, ClassNotFoundException {
		list();

		sendData(new Message(MessageType.DETAILS));

		System.out.print("Which one? [filename] ");
		String filename = stdin.nextLine();
		Message request = new Message(MessageType.DETAILS);
		request.description = filename;
		sendData(request);
		Message response = (Message) input.readObject();
		if (response.status) {
			System.out.println(filename + "\n==========");
			for (ContactInfo info: response.details)
				System.out.println(info);
			lastRequestedFilename = filename;
		} else {
			System.out.println(response.description);
		}
		return response.details; /* null, when negative response */
	}

	/*
	 * Informs the tracker about new shared files and increments the tracker's
	 * download/failure counters for the specified peer, according to whether
	 * or not them sending a file was successful.
	 */
	void notifyTracker(ContactInfo peer, boolean success) throws IOException, ClassNotFoundException {
		sendData(new Message(MessageType.INFORM));
		inform();

		sendData(new Message(MessageType.NOTIFY));
		Message notification = new Message(success, MessageType.NOTIFY);
		notification.peer = peer;
		notification.tokenID = tokenID;
		sendData(notification);
	}

	/*
	 * After getting details for the specified file, compute the peer for the
	 * optimal download, then request the file from that peer.
	 */
	boolean simpleDownload() throws IOException, ClassNotFoundException {
		List<ContactInfo> peers = details();
		if (peers == null || peers.isEmpty())
			return false;

		/* if the file is already shared, don't bother */
		File reqFile = new File(sharedDir, lastRequestedFilename);
		if (reqFile.exists()) {
			System.out.println(sharedDir + " already contains '" +
					lastRequestedFilename + "'");
			return true;
		}

		/*
		 * Measure response times for each peer returned from calling details.
		 * Active peers get scored according to their downloads and failures
		 * counts.
		 *
		 * TODO: need to keep all scores (sorted in ascending order)
		 */
		double bestScore = Double.MAX_VALUE;
		ContactInfo bestPeer = peers.get(0);
		for (int i = 0; i < peers.size(); ++i) {
			ContactInfo peer = peers.get(i);
			Instant start = Instant.now();
			boolean active = checkActivePeer(peer);
			Instant finish = Instant.now();
			if (active) {
				double score =
					(double) Duration.between(start, finish).toMillis() *
					Math.pow(.75d, peer.countDownloads) *
					Math.pow(1.25d, peer.countFailures);
				if (score < bestScore) {
					bestScore = score;
					bestPeer = peer;
				}
				System.out.println(peer.username + " = " + score +
						" (current best = " + bestScore + ")");
			}
		}

		/* establish connection to optimal peer */
		boolean success = false;
		try {
			System.out.println("Establishing connection to " + bestPeer.username + "...");
			Socket tmpSock = new Socket(bestPeer.getIP(), bestPeer.port);
			ObjectInputStream in = new
				ObjectInputStream(tmpSock.getInputStream());
			ObjectOutputStream out = new
				ObjectOutputStream(tmpSock.getOutputStream());
			Message request = new Message(MessageType.DOWNLOAD);
			request.description = lastRequestedFilename;
			out.writeObject(request);
			out.flush();
			Message response = (Message) in.readObject();
			if (response.status) {
				/* read the file in 4K byte chunks */
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(reqFile));
				int bytesRead;
				byte[] bytes = new byte[4096];
				while ((bytesRead = in.read(bytes, 0, bytes.length)) > 0)
					bos.write(bytes, 0, bytesRead);
				bos.close();
				System.out.println("Received '" + lastRequestedFilename +
						"' from peer: " + bestPeer.username);
				success = true;
			} else {
				success = false;
				System.out.println(tmpSock + ": " + response.description);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* updateSharedFiles gets called in here */
		notifyTracker(bestPeer, success);
		return success;
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
			System.out.print("Updating shared files (" + sharedDir + ")... ");
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
					case "D":
						simpleDownload();
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
