import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Peer implements IPeer {
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

	public static void main(String[] args) {
		// TODO add scope to these variables
		int id = 1;
		String sharedDir = "shared_directory_" + id;
		String fileDownloadList = "fileDownloadList.txt";
		ArrayList<File> sharedFiles = new ArrayList<>();

		// check which files from fileDownloadList we have
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileDownloadList));
			String filename;
			while ((filename = reader.readLine()) != null) {
				File sharedFile = new File(sharedDir + "/" + filename);
				if (sharedFile.exists()) {
					sharedFiles.add(sharedFile);
				}
			}
			// verification
			if (!sharedFiles.isEmpty()) {
				for (File shareFile: sharedFiles) {
					System.out.println(shareFile);
				}
			} else {
				System.out.println("No shared files in " + sharedDir);
			}
		} catch (IOException e) {
			System.err.println("Could not open " + fileDownloadList);
		}

		// establish a connection
		try {
			// Connect to server
			Socket connection = new Socket("localhost", 9090);

			// get Input and Output streams
			ObjectOutputStream out = new
				ObjectOutputStream(connection.getOutputStream());
			ObjectInputStream in = new
				ObjectInputStream(connection.getInputStream());

			//send message get it back and print it
			Message message = new Message(1);
			message.username = "george";
			message.password = "stamoulis";
			out.writeObject(message);
			out.flush();

			// 'reply' was previously 'message'
			String reply = (String) in.readObject();
			System.out.println(reply);

			connection.close();
		} catch (Exception e) {
			System.err.println("Error occurred.");
			e.printStackTrace();
		}
	}
}
