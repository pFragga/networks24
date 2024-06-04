import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Message implements Serializable {
	private static final long serialVersionUID = 42L; // IGNORE
	List<String> runningFiles;

	boolean status;
	MessageType type;
	String description;
	String password;
	String tokenID;
	String username;
	int listeningPort;
	ArrayList<String> sharedFilesNames;
	ArrayList<ContactInfo> details;
	Map<String, List<File>> Pieces;

	/* the peer who successfully sent a file upon a download request */
	ContactInfo peer;

	/* this is the default message */
	Message() {
		this.status = true;
		this.type = MessageType.GENERIC;
	}

	/* we can add more constructors for the message here */

	Message(MessageType type) {
		this.status = true;
		this.type = type;
	}

	Message(boolean status, MessageType type) {
		this.status = status;
		this.type = type;
	}
}
