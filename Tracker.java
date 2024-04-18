import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Tracker {
	private void listen(int port) {
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				new Thread(new TrackerThread(clientSocket)).start();
			}
		} catch (IOException e) {
			System.err.println("Connection aborted.");
		}
	}

	public static void main(String[] args) {
		Tracker tracker = new Tracker();
		tracker.listen(9090);
	}
}
