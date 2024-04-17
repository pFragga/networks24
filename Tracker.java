import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.ServerSocket;

public class Tracker implements ITracker {
	@Override
	public void register() {
	}

	@Override
	public void login() {
	}

	@Override
	public void logout() {
	}

	@Override
	public void respondToNotify() {
	}

	@Override
	public void reply_list() {
	}

	@Override
	public void reply_details() {
	}

	public static void main(String[] args) {
		try {
			ServerSocket serverSocket = new ServerSocket(9090);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				new TrackerThread(clientSocket).start();
			}
		} catch (Exception e) {
			System.err.println("Error occurred. Aborting...");
			e.printStackTrace();
		}
	}
}
