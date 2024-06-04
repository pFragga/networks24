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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
	Map<String, List<File>> filenamesToPieces;

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
		this.filenamesToPieces = new ConcurrentHashMap<>();
		this.peerServer = new PeerServer(this);
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

	void seederInform() throws IOException, ClassNotFoundException {
		if (!connected) {
			System.out.println("You need to be connected first.");
			return;
		}

		if (tokenID == null || tokenID.isEmpty()) {
			System.out.println("You need to login first.");
			return;
		}

		/* in case a new file has been added in the meantime... */
		updateSharedFiles();

		// Create a HashMap to store pieces
		Map<String, List<File>> Pieces = new HashMap<>();

		// Iterate through the entries of the filenamesToPieces map
		for (Map.Entry<String, List<File>> entry : filenamesToPieces.entrySet()) {
			String key = entry.getKey(); // Get the filename without extension
			List<File> fileList = entry.getValue(); // Get the list of files

			// Check if the filename exists as a key in the Pieces map
			if (Pieces.containsKey(key)) {
				// If the key exists, add the files to the corresponding list
				Pieces.get(key).addAll(fileList);
			} else {
				// If the key does not exist, add the files to a new list and put it in the Pieces map
				Pieces.put(key, new ArrayList<>(fileList));
			}
		}

		// Inform the tracker about the communication information
		Message seederInfo = new Message(MessageType.SEEDER_INFORM);
		seederInfo.tokenID = tokenID;
		seederInfo.Pieces = Pieces; // Get the port the peer server is listening on
		sendData(seederInfo);

		// Receive response from the tracker
		Message response = (Message) input.readObject();
		if (response.status) {
			System.out.println("Successfully informed tracker about seeder capabilities.");
		} else {
			System.out.println("Failed to inform tracker about seeder capabilities. Reason: " + response.description);
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
			seederInform();
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
	/*List<ContactInfo> details() throws IOException, ClassNotFoundException {
		if (!connected) {
			System.out.println("You are already not connected.");
			return null;
		}

		if (tokenID == null || tokenID.isEmpty()) {
			System.out.println("You have not yet logged in.");
			return null;
		}

		sendData(new Message(MessageType.DETAILS));

		System.out.print("Enter the filename: ");
		String filename = stdin.nextLine();
		Message request = new Message(MessageType.DETAILS);
		request.description = filename;
		sendData(request);
		Message response = (Message) input.readObject();

		if (response.status) {
			System.out.println("Contact information for peers having pieces of '" + filename + "':");
			if (response.details.isEmpty()) {
				System.out.println("No peers found with pieces of '" + filename + "'.");
			} else {
				for (ContactInfo info : response.details) {
					System.out.println("Peer: " + info.username);
					System.out.println("IP Address: " + info.getIP());
					System.out.println("Port: " + info.port);
					System.out.println("Download Count: " + info.countDownloads);
					System.out.println("Failure Count: " + info.countFailures);
					System.out.println("Number of Pieces: " + info.numPieces);
					System.out.println("Seeder: " + (info.isSeeder ? "Yes" : "No"));
					System.out.println();
				}
			}
		} else {
			System.out.println(response.description);
		}
		return response.details; /* null, when negative response */
	

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

	String getBasename(String filename) {
		String[] splits = filename.split("\\.txt"); /* TODO */
		String basename = "";
		for (int i = 0; i <= splits.length - 1; ++i)
			basename += splits[i];
		return basename;
	}

	List<File> partition(File f, int numPieces) {
		if (!fileOK(f))
			return null;

		if (f.isDirectory()) {
			System.err.println("Input file cannot be a directory.");
			return null;
		}

		long flength = f.length();

		if (flength > Integer.MAX_VALUE) {
			System.err.println("Input file too large to split.");
			return null;
		}

		String filename = f.getName();
		String basename = getBasename(filename);
		int bytesRead = 0;
		List<File> pieces = new ArrayList<>();
		try {
			BufferedInputStream bis = new BufferedInputStream(new
					FileInputStream(f));
			for (int i = 1; i <= numPieces; ++i) {
				int plength = (int) flength / numPieces;

				/*
				 * If the length of the file (in bytes) cannot be divided
				 * evenly, make the last piece a bit bigger in order to make up
				 * for the remainder.
				 */
				if (i == numPieces)
					plength += (int) flength % numPieces;

				/* read the file's bytes into a buffer */
				byte[] buffer = new byte[plength];
				bytesRead += bis.read(buffer, 0, plength);

				/* write buffer into a new file with the apropriate suffix */
				File piece = new File(sharedDir, basename + "-" + i + ".part");
				BufferedOutputStream bos = new BufferedOutputStream(new
						FileOutputStream(piece));
				System.out.print("Creating piece '" + piece.getName() + "'... ");
				bos.write(buffer, 0, plength);
				System.out.println("OK.");
				pieces.add(piece);

				bos.close();
			}
			bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return pieces;
	}

	boolean fileOK(File f) {
		if (f == null) {
			System.err.println("Provided file was null pointer.");
			return false;
		}

		if (!f.exists()) {
			System.err.println("Could not find '" + f.getName() + "'.");
			return false;
		}

		return true;
	}

	File assemble(List<File> parts, String filename) {
		if (parts == null) {
			System.err.println("Provided parts list was null pointer.");
			return null;
		}

		if (parts.isEmpty()) {
			System.err.println("Provided parts list was empty.");
			return null;
		}

		try {
			File assembledFile = new File(sharedDir, filename);
			BufferedOutputStream bos = new BufferedOutputStream(new
					FileOutputStream(assembledFile));
			for (File part: parts) {
				byte[] bytes = Files.readAllBytes(part.toPath());
				bos.write(bytes, 0, (int) part.length());
			}
			bos.close();
			return assembledFile;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	boolean cmpFiles(File f1, File f2) {
		if (!fileOK(f1))
			return false;
		if (!fileOK(f2))
			return false;

		if (f1.isDirectory() || f2.isDirectory()) {
			System.err.println("Input files must not be directories.");
			return false;
		}

		try {
			byte[] a = Files.readAllBytes(f1.toPath());
			byte[] b = Files.readAllBytes(f2.toPath());
			return Arrays.equals(a, b);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	void updateSharedFiles() {
		try {
			BufferedReader reader = new BufferedReader(new
					FileReader("fileDownloadList.txt"));
			System.out.println(">>> Updating shared files (" + sharedDir + ") <<<");
			String filename;
			while ((filename = reader.readLine()) != null) {
				File currFile = new File(sharedDir, filename);
				if (!currFile.exists() || sharedFiles.contains(currFile))
					continue;
				List<File> pieces = partition(currFile, 10);
				if (pieces == null)
					continue;
				sharedFiles.add(currFile);
				filenamesToPieces.put(currFile.getName(), pieces);
			}
			System.out.println(">>> Update finished <<<");
		} catch (IOException e) {
			e.printStackTrace();
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
