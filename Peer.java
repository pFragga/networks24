import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.net.Socket;
import java.util.Scanner;

public class Peer {
	private static final Scanner stdin = new Scanner(System.in);

	/*
	 * Print a summary of each function of the commandline interface to
	 * stdout.
	 *
	 * Use this, whenever the user inputs something invalid/incorrect.
	 */
	private static void showUsage() {
		System.out.println("usage: blah blah blah..."); // TODO
	}

	private static void connect(String ipaddr, int port) {
		try {
			Socket connection = new Socket(ipaddr, port);
			ObjectOutputStream out = new
				ObjectOutputStream(connection.getOutputStream());
			ObjectInputStream in = new
				ObjectInputStream(connection.getInputStream());

			/* send stuff to the tracker */
			out.writeObject(new Message("my dirty little secret", 1));
			out.flush();

			/* receive stuff from tracker */
			String message = (String) in.readObject();
			System.out.println(message);

			/*
			 * TODO
			 * if tracker response == blah blah, open a server
			 * socket to connect to another peer
			 */

			connection.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Connection aborted.");
			System.exit(1);
		}
	}

	/*
	 * Basic idea
	 * ==========
	 * The peer (client) program should provide a commandline interface for
	 * all it's functionality. Each function should be assigned a unique
	 * letter, e.g. h -> help, etc.
	 */
	public static void main(String[] args) {
		System.out.println("Welcome to our simple P2P file sharing system!");
		while (true) {
			System.out.print("(h for help)> ");
			String input = stdin.next();
			switch (input) {
				case "c":
					connect("localhost", 9090); // TODO
					break;
				case "h":
					showUsage();
					break;
				case "q":
					stdin.close();
					System.exit(0);
				default:
			}
			System.out.println();
		}
	}
}
