import java.io.Serializable;

/*
 * maybe make generic: <T>
 */
public class Message implements Serializable {
	/*
	 * 1 => registration credentials (String username, String password)
	 * 2 => login
	 * 3 => logout
	 * 4 => logout
	 * 5 => request download
	 * ...
	 */
	int msg_type;
	boolean toPeer = false;
	String username = "";
	String password = "";
	String token_id = "";
	boolean status = false; // success/failure
	String ipaddr = "";
	int port = 0;
	boolean peer_active = false; // checkActive()

	Message(int msg_type) {
		this.msg_type = msg_type;
	}

	Message(int msg_type, boolean toPeer) {
		this.msg_type = msg_type;
		this.toPeer = toPeer;
	}
}
