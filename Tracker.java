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

	void postUpdateDataStructures() {
		System.out.println("UPDATED DATA STRUCTURES:\n" +
				"activePeers = " + activePeers + "\n" +
				"allFilenames = " + allFilenames + "\n" +
				"filenamesToTokenIDs = " + filenamesToTokenIDs);
	}

	synchronized void updateAllFilenames(String filename) {
		allFilenames.add(filename);
	}

	synchronized void updateActivePeers(String tokenID) {
		activePeers.remove(tokenID);
	}

	synchronized void updateActivePeers(String tokenID, ContactInfo info) {
		activePeers.put(tokenID, info);
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
				new Thread(new TrackerThread(this, connection)).start();
			} while (listening);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			serverCleanup();
		}
	}
}
