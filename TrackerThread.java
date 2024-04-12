import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;
import java.lang.InterruptedException;
import java.lang.Thread;
import java.net.Socket;

public class TrackerThread extends Thread {
	private static int id = 1;
	private Socket csocket;

	public TrackerThread(Socket csocket) {
		super("TrackerThread" + id++); // see: Thread.name
		this.csocket = csocket;
	}

	@Override
	public void run() {
		System.out.println("Thread '" + this.getName() + "' accepted a new connection: " + csocket.toString());
		try {
			ObjectOutputStream out = new
				ObjectOutputStream(csocket.getOutputStream());
			ObjectInputStream in = new
				ObjectInputStream(csocket.getInputStream());
			Message message = (Message) in.readObject();
			Thread.sleep(2000); // pretend to be working hard lmao
			switch (message.type) {
			case 1:
				out.writeObject("Secret: " + message.secret);
				break;
			case 2:
				out.writeObject(csocket.getInetAddress().toString());
				break;
			default:
				out.writeObject("Unknown message type.");
			}
			out.flush();
			csocket.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Received invalid data: Aborting.");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("I/O error: Aborting.");
			System.exit(1);
		}
	}
}
