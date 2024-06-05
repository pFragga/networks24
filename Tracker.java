import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class Tracker {
	boolean listening;
	ServerSocket ssocket;
	Map<String, User> registeredPeersInfo;
	Map<String, ContactInfo> activePeers;
	Map<String, Set<String>> filenamesToTokenIDs;

	Tracker() {
		registeredPeersInfo = new ConcurrentHashMap<>();
		activePeers = new ConcurrentHashMap<>();
		filenamesToTokenIDs = new ConcurrentHashMap<>();
	}

	void postUpdateDataStructures() {
		System.out.println("UPDATED DATA STRUCTURES:\n" +
				"activePeers = " + activePeers + "\n" +
				"filenamesToTokenIDs = " + filenamesToTokenIDs);
	}

	synchronized void deactivatePeer(String tokenID) {
		activePeers.remove(tokenID);
	}

	synchronized void activatePeer(String tokenID, ContactInfo info) {
		activePeers.put(tokenID, info);
	}

	/*
	 * Unmaps given peer's tokenID from all mappings that contain it. Used
	 * whenever a peer is wants to logout.
	 */
	synchronized void updateFilenamesToTokenIDs(String tokenID) {
		for (Set<String> tokenIDs : filenamesToTokenIDs.values()) {
			tokenIDs.remove(tokenID);
		}
	}

	/*
	 * Updates the filename->tokenID mappings by either appending to an already
	 * existing mapping or creating a new one.
	 */
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

	public synchronized void updateFilenamePiecesToTokenIDs(String filename, String tokenID) {
		Set<String> tokenIDs = filenamesToTokenIDs.computeIfAbsent(filename, k -> new HashSet<>());
		tokenIDs.add(tokenID);
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
				new Thread(new TrackerThread(this, connection)).start();
			} while (listening);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			serverCleanup();
		}
	}
}
