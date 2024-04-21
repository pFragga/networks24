import java.io.Serializable;
import java.util.ArrayList;

/*
 * maybe make generic: <T>
 */
public class Message implements Serializable {
	private static final long serialVersionUID = 42L; // ignore this

	ArrayList<String> availableFiles = null;
	boolean peer_active = false; // checkActive()
	boolean status = true; // success/failure
	boolean toPeer = false;
	int msg_type;
	int port;
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
