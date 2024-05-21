import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
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
import java.util.Collections;
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
	String lastRequestedFilename;

	/* server tracker's and other peers' activity and download requests */
	PeerServer peerServer;

	/* tracker info */
	int trackerPort;
	String trackerHost;

	Peer(String trackerHost, int trackerPort, String sharedDir) {
		this.trackerHost = trackerHost;
		this.trackerPort = trackerPort;
		this.sharedFiles = new ArrayList<>();
		this.sharedDir = sharedDir;
		peerServer = new PeerServer(this);
	}

	void getHelp() {
		System.out.println(
				"[c]\tconnect to tracker\n" +
				"[d]\tdisconnect from tracker\n\n" +
				"[r]\tregister (requires connection)\n" +
				"[l]\tlogin (requires connection)\n" +
				"[L]\tlogout (requires connection)\n\n" +
				"[ls]\tlist tracker's known files\n" +
				"[Q]\tquery details about given file\n" +
				"[ch]\tcheck if the tracker (or a peer) is active\n\n" +
				"[D]\tdownload given file\n" +
				"[?]\tlist files in your shared directory\n" +
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

		if (registered && (tokenID != null && !tokenID.isEmpty())) {
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
		credentials.listeningPort = peerServer.listeningPort;
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
		try {
			sendData(new Message(MessageType.ACTIVE));
		} catch (IOException e) {
			System.out.println("inactive.");
			return false;
		}

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
		System.out.print("Checking status for " + peer.username + "...");

		/*
		 * If we get IOException when opening the socket, that means the peer
		 * is definitely inactive and we can early return.
		 */
		Socket tmpSock;
		try {
			tmpSock = new Socket(peer.getIP(), peer.port);
		} catch (IOException e) {
			System.out.println("inactive.");
			return false;
		}

		/* establish connection to selected peer */
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
			tokenID = null;
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
	 * Measure response times for each peer by measuring how long they take
	 * to respond to a checkActive call. Active peers get scored according
	 * to their downloads and failures counts.
	 */
	List<ContactInfo> scorePeers(List<ContactInfo> peers) throws IOException, ClassNotFoundException {
		for (int i = 0; i < peers.size(); ++i) {
			ContactInfo peer = peers.get(i);
			Instant start = Instant.now();
			boolean active = checkActivePeer(peer);
			Instant finish = Instant.now();
			if (active) {
				peer.score =
					(double) Duration.between(start, finish).toMillis() *
					Math.pow(.75d, peer.countDownloads) *
					Math.pow(1.25d, peer.countFailures);
				System.out.println("Peer " + peer.username + " scored " + peer.score);
			} else {
				peers.remove(peer);
			}
		}
		Collections.sort(peers);
		return peers;
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
		if (sharedFiles.contains(reqFile)) {
			System.out.println(sharedDir + " already contains '" +
					lastRequestedFilename + "'");
			return true;
		}

		peers = scorePeers(peers);
		if (peers.isEmpty()) {
			System.out.println("No active peers were left");
			return false;
		}

		boolean success = false;
		int i;
		for (i = 0; i < peers.size(); ++i) {
			ContactInfo bestPeer = peers.get(i);
			System.out.println("Establishing connection to " + bestPeer.username + "...");
			Socket tmpSock = new Socket(bestPeer.getIP(), bestPeer.port);
			ObjectInputStream in = new
				ObjectInputStream(tmpSock.getInputStream());
			ObjectOutputStream out = new
				ObjectOutputStream(tmpSock.getOutputStream());

			/* request the file */
			Message request = new Message(MessageType.DOWNLOAD);
			request.description = lastRequestedFilename;
			out.writeObject(request);
			out.flush();

			/* handle response */
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
				System.out.println(tmpSock + ": " + response.description);
				success = false;
			}

			/* cleanup */
			tmpSock.close();
			in.close();
			out.close();

			/* updateSharedFiles gets called in here */
			notifyTracker(bestPeer, success);

			if (success)
				break;
		}

		return success;
	}

	void listSharedDir() {
		try {
			File dir = new File(sharedDir);
			for (String filename: dir.list())
				System.out.println(filename);
		} catch (Exception e) {
			e.printStackTrace();
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

	void begin() {
		running = true;
		updateSharedFiles();
		connect(trackerHost, trackerPort); /* attempt connection on startup */
		new Thread(peerServer).start();
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
					case "ch":
						checkActive();
						break;
					case "D":
						simpleDownload();
						break;
					case "?":
						listSharedDir();
						break;
					case "h":
						getHelp();
						break;
					case "q":
						if (tokenID != null && !tokenID.isEmpty())
							logout();
						if (connected)
							disconnect();
						if (!peerServer.ssocket.isClosed())
							peerServer.ssocket.close();
						running = false;
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
