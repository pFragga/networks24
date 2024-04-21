import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Peer implements IPeer {
	private int id;
	private String sharedDir;
	private String fileDownloadList;
	private ArrayList<File> sharedFiles;
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private static Scanner stdin;
	private static boolean running;
	private Socket connection;

	public Peer() {
		id++;
		this.sharedDir = "shared_directory_" + id; // FIXME
		this.fileDownloadList = "fileDownloadList.txt";
		this.sharedFiles = new ArrayList<>();
		stdin = new Scanner(System.in);
		running = true;
	}

	@Override
	public void list() {
		try {
			connect("localhost", 9090);
			// send request
			Message message = new Message(10);
			out.writeObject(message);
			out.flush();

			// handle response
			Message reply = (Message) in.readObject();
			if (reply.status) {
				System.out.println("Available files:\n================");
				for (String filename: reply.availableFiles) {
					System.out.println(filename);
				}
			} else {
				System.out.println("Could not get file listing.");
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Invalid message received. Aborting...");
		} catch (IOException e) {
			System.err.println("Connection error. Aborting...");
		}
	}

	/**
	 * Request details for a specific file that the tracker knows about.
	 *
	 * If the tracker successfully finds at least one peer that hold  that
	 * file, we receive a list of information about that specific peer.
	 */
	@Override
	public void details() {
		System.out.print("Which file do you want details for?> ");
		String filename = stdin.next();
		try {
			connect("localhost", 9090);
			Message message = new Message(11);
			message.requestedFileName = filename;
			out.writeObject(message);
			out.flush();

			Message reply = (Message) in.readObject();
			if (reply.status) {
				System.out.println("Peer(s) found:\n==============");
				for (User provider : reply.providersForReqFile) {
					System.out.println(provider); // calls User.toString()
				}
			} else {
				System.out.println("No peer(s) for " + filename);
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Invalid message received. Aborting...");
		} catch (IOException e) {
			System.err.println("Connection error. Aborting...");
		}
	}

	@Override
	public void checkActive() {
	}

	@Override
	public void simpleDownload() {
	}

	@Override
	public void notifyTracker() {
	}

	@Override
	public void inform() {
	}

	@Override
	public void register() {
	}

	@Override
	public void login() {
	}

	@Override
	public void logout() {
	}

	private void showUsage() {
		System.out.print(
				"[r]\tregister\n" +
				"[l]\tlogin\n" +
				"[L]\tlogout without quiting\n\n" +
				"[d]\tget details for a certain file\n" +
				"[ls]\tlist tracker's known files\n\n" +
				"[h]\tshow usage\n" +
				"[q]\tlogout and quit\n"
				);
	}

	private void updateSharedFiles() {
		// check which files from fileDownloadList we have
		try {
			System.out.print("Updating shared files... ");
			BufferedReader reader = new BufferedReader(new FileReader(fileDownloadList));
			String filename;
			while ((filename = reader.readLine()) != null) {
				File sharedFile = new File(sharedDir + "/" + filename);
				if (sharedFile.exists() && !sharedFiles.contains(sharedFile))
					sharedFiles.add(sharedFile);
			}
			//if (!sharedFiles.isEmpty()) {
			//	System.out.println();
			//	for (File shareFile: sharedFiles) {
			//		System.out.println(shareFile);
			//	}
			//}
			System.out.println("OK.");
		} catch (IOException e) {
			System.err.println("Could not open " + fileDownloadList);
		}
	}

	private void connect(String ipaddr, int port) {
		try {
			connection = new Socket(ipaddr, port);
			out = new ObjectOutputStream(connection.getOutputStream());
			in = new ObjectInputStream(connection.getInputStream());
		} catch (IOException e) {
			System.err.println("Connection error. Aborting...");
		}
	}

	private void disconnect() {
		try {
			if (connection != null && !connection.isClosed())
				connection.close();
		} catch (IOException e) {
			System.err.println("Connection error. Aborting...");
		}
	}

	public static void main(String[] args) {
		System.out.println("Welcome to our simple P2P file sharing system!");
		Peer peer = new Peer();
		peer.updateSharedFiles();
		while (running) {
			System.out.print("(h for help)> ");
			String input = stdin.next();
			switch (input) {
				case "r":
					peer.register();
					break;
				case "l":
					peer.login();
					break;
				case "L":
					peer.logout();
					break;
				case "d":
					peer.details();
					break;
				case "ls":
					peer.list();
					break;
				case "h":
					peer.showUsage();
					break;
				case "q":
					peer.disconnect();
					running = false;
					break;
				default:
					peer.showUsage();
			}
		}

		stdin.close();
		peer.disconnect(); // just in case...
	}
}
