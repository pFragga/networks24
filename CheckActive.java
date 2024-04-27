import java.util.Map;

class CheckActive {
    private Map<String, ContactInfo> activePeers;


    public CheckActive(String peerTokenID, ContactInfo info){
            this.peerTokenID = peerTokenID;
            this.info = info;
    }
    // Method to check if a peer is active
    private boolean isActivePeer(String peerTokenID) {
        return activePeers.containsKey(peerTokenID);
    }

    // Method to respond to checkActive messages
    public void respondToCheckActive(String peerTokenID) {
        if (isActivePeer(peerTokenID)) {
            // Peer is active, send confirmation message
            Message response = new Message(true, MessageType.CHECK_ACTIVE_RESPONSE);
            sendMessageToPeer(peerTokenID, response);
        } else {
            // Peer is not active, do nothing or send failure message
            // Message failureResponse = new Message(false, MessageType.CHECK_ACTIVE_RESPONSE);
            // sendMessageToPeer(peerTokenID, failureResponse);
        }
    }

    // Method to handle checkActive messages received from other peers or the tracker
    public void handleCheckActive(Message message) {
        String peerTokenID = message.tokenID;
        respondToCheckActive(peerTokenID);
    }
}