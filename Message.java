import java.io.Serializable;

class Message implements Serializable {
	int type = 1; // default value: 1
	String secret;

	Message(String secret) {
		this.secret = secret;
	}

	Message(String secret, int type) {
		this.secret = secret;
		this.type = type;
	}
}
