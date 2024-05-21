import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

class PeerThread implements Runnable {
	Socket socket;
	ObjectOutputStream out;
	ObjectInputStream in;
	Peer peer;

	PeerThread(Peer peer, Socket socket) {
		this.peer = peer;
		this.socket = socket;
	}

	boolean simpleDownload(Message request) throws IOException {
		String filename = request.description;
		File f = new File(peer.sharedDir, filename);
		long flength = f.length();

		/*
		 * first check if we can actually send the file, then inform the
		 * requesting peer
		 */
		Message response;
		if (!f.exists() || flength > Integer.MAX_VALUE) {
			response = new Message(false, MessageType.DOWNLOAD);
			response.description = "Could not send '" + filename + "' to " + socket;
			System.out.println(response.description);
			out.writeObject(response);
			out.flush();
			return false;
		}
		response = new Message(MessageType.DOWNLOAD);
		System.out.println(socket + ": requested '" + filename + "'");
		out.writeObject(response);
		out.flush();

		byte[] bytes = new byte[(int) flength];
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
		bis.read(bytes, 0, bytes.length);
		out.write(bytes, 0, bytes.length);
		out.flush();
		bis.close();
		System.out.println("Sent '" + filename + "' to " + socket);
		return true; /* TODO */
	}

	void checkActive(Message request) throws IOException {
		Message response = new Message(peer.running, MessageType.ACTIVE);
		out.writeObject(response);
		out.flush();
	}

	@Override
	public void run() {
		try {
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			Message request = (Message) in.readObject();
			switch (request.type) {
				case ACTIVE:
					checkActive(request);
					break;
				case DOWNLOAD:
					simpleDownload(request);
					break;
				default:
					System.err.println("Received unknown message type");
					break;
			}

			// Close streams and socket
			out.close();
			in.close();
			socket.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
