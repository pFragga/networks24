import java.io.Serializable;

/*
 * maybe make generic: <T>
 */
public class Message implements Serializable {
	boolean peer_active = false; // checkActive()
	boolean status = false; // success/failure
	boolean toPeer = false;
	int msg_type;
	int port = 0;
	String ipaddr = "";
	String password = "";
	String token_id = "";
	String username = "";

	/*
	 * 1 => registration credentials (String username, String password)
	 * 2 => login
	 * 3 => logout
	 * 4 => logout
	 * 5 => request download
	 * ...
	 */
	Message(int msg_type) {
		this.msg_type = msg_type;
	}

	Message(int msg_type, boolean toPeer) {
		this.msg_type = msg_type;
		this.toPeer = toPeer;
	}
}
