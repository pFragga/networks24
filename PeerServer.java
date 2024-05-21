import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class PeerServer implements Runnable {
	int listeningPort;
	ServerSocket ssocket;
	Peer peer;

	PeerServer(Peer peer) {
		/* choose a random port between 10000 and 25000 */
		this.listeningPort = (int) (Math.random() * (25000 - 10000) + 10000);
		this.peer = peer;
	}

	void serverCleanup() {
		try {
			if (ssocket != null && !ssocket.isClosed())
				ssocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			ssocket = new ServerSocket(listeningPort, 20);
			while (peer.running) {
				Socket socket = ssocket.accept();
				new Thread(new PeerThread(peer, socket)).start();
			}
		} catch (IOException e) {
			System.out.println("Peer server closed.");
		} finally {
			serverCleanup();
		}
	}
}
