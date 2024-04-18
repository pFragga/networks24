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
	private static Socket connection;

	public Peer() {
		this.id++;
		this.sharedDir = "shared_directory_" + id; // FIXME
		this.fileDownloadList = "fileDownloadList.txt";
		this.sharedFiles = new ArrayList<>();
		this.stdin = new Scanner(System.in);
		this.running = true;
	}

	@Override
	public void list() {
	}

	@Override
	public void details() {
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
				"[l]\tlist tracker's known files\n\n" +
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
				if (sharedFile.exists() && !sharedFiles.contains(sharedFile)) {
					sharedFiles.add(sharedFile);
				}
			}
			//if (!sharedFiles.isEmpty()) {
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
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
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
				case "l":
					peer.list();
					break;
				case "r":
					peer.register();
					break;

				/* TODO: add more here */

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
