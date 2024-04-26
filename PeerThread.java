import java.io.*;
import java.net.*;

class PeerThread extends Thread {
    private ServerSocket serverSocket;
    private Peer peer;

    public PeerThread(Peer peer, int port) throws IOException {
        this.peer = peer;
        this.serverSocket = new ServerSocket(port);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleConnection(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Error in PeerThread: " + e.getMessage());
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing PeerThread server socket: " + e.getMessage());
            }
        }
    }

    private void handleConnection(Socket clientSocket) {
        try (
                ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            Message request = (Message) input.readObject();
            // Handle the request based on its type
            switch (request.getType()) {
                case DOWNLOAD_REQUEST:
                    /* Example: Handle file download request */
                    String fileName = request.getDescription();
                    // Implement logic to send the requested file to the peer
                    // For simplicity, let's assume a method sendFile() is implemented in the Peer class
                    peer.sendFile(fileName, output);
                    break;
                // Add more cases for handling other types of requests
                default:
                    // Handle unsupported request type
                    System.err.println("Unsupported request type received.");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling connection in PeerThread: " + e.getMessage());
        }
    }
}