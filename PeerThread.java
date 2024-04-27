import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class PeerThread implements Runnable {
    private ServerSocket serverSocket;
    private boolean running;
    private Peer peer;

    public PeerThread(Peer peer, int port) {
        try {
            this.peer = peer;
            this.serverSocket = new ServerSocket(port);
            System.out.println("Peer server started on port " + port);
        } catch (IOException e) {
            System.err.println("Error starting peer server: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                // Accept incoming connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection accepted from " + clientSocket.getInetAddress());
                // Handle the connection in a new thread here or put handle logic there
                //                     |
                //                     |
                //                     ↓



            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e.getMessage());
            }
        }
    }
    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error stopping peer server: " + e.getMessage());
        }
    }
}