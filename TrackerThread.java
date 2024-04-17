import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Thread;
import java.net.Socket;

public class TrackerThread extends Thread {
	private static int id = 1;
	private Socket clientSocket;

	public TrackerThread(Socket clientSocket) {
		super("TrackerThread" + id++); // see: Tracker.name
		this.clientSocket = clientSocket;
	}

	public static void greetUser(ObjectOutputStream out, Message message) {
		try {
			out.writeObject("Welcome, " + message.username + "!!");
			out.flush();
		} catch (Exception e) {
			System.err.println("Could not welcome user. :(");
		}
	}

	@Override
	public void run() {
		System.out.println("Thread '" + this.getName() + "' accepted a new connection: "
				+ clientSocket.toString());
		try {
			// get Input and Output streams
			ObjectOutputStream out = new
				ObjectOutputStream(clientSocket.getOutputStream());
			ObjectInputStream in = new
				ObjectInputStream(clientSocket.getInputStream());

			Message message = (Message) in.readObject();
			switch (message.msg_type) {
				case 1:
					greetUser(out, message);
					break;
				default:
			}
			out.flush(); // ensure flushed output before closing
			clientSocket.close();
		} catch (Exception e) {
			System.err.println("Error occurred. Aborting...");
			e.printStackTrace();
		}
	}
}
