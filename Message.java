import java.io.Serializable;


class Message implements Serializable {
	private static final long serialVersionUID = 42L; // IGNORE

	boolean status;
	MessageType type;
	String description;
	String password;
	String tokenID;
	String username;

	/* this is the default message */
	Message() {
		this.status = true;
		this.type = MessageType.GENERIC;
	}

	/* we can add more actors for the message here */


	Message(MessageType type) {
		this.status = true;
		this.type = type;
	}

	Message(boolean status, MessageType type) {
		this.status = status;
		this.type = type;
	}
}
