import java.io.Serializable;
import java.util.ArrayList;

class Message implements Serializable {
	private static final long serialVersionUID = 42L; // IGNORE

	boolean status;
	MessageType type;
	String description;
	String password;
	String tokenID;
	String username;
	int listeningPort;
	ArrayList<String> sharedFilesNames;
	ArrayList<ContactInfo> details;

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
