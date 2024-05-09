import java.io.Serializable;
import java.net.InetAddress;

class ContactInfo implements Serializable {
	private static final long serialVersionUID = 43L; // IGNORE

	InetAddress ipAddr;
	int countDownloads;
	int countFailures;
	int port;
	String tokenID;
	String username;

	ContactInfo(InetAddress ipAddr, int port, String tokenID, String username) {
		this.ipAddr = ipAddr;
		this.port = port;
		this.tokenID = tokenID;
		this.username = username;
		this.countDownloads = 0;
		this.countFailures = 0;
	}

	String getIP() {
		return ipAddr.getHostAddress();
	}

	@Override
	public String toString() {
		return "{" + username + " [ID=" + tokenID + ",DL=" + countDownloads + ",FL="
			+ countFailures + "] -> (" + getIP() + ":" + port +
			")}";
	}
}
