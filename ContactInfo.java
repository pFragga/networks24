import java.io.Serializable;
import java.lang.Comparable;
import java.net.InetAddress;

class ContactInfo implements Serializable, Comparable<ContactInfo> {
	private static final long serialVersionUID = 43L; // IGNORE

	String ipAddr;
	int countDownloads;
	int countFailures;
	int port;
	String tokenID;
	String username;
	double score;

	ContactInfo(InetAddress ipAddr, int port, String tokenID, String username) {
		this.ipAddr = ipAddr;
		this.port = port;
		this.tokenID = tokenID;
		this.username = username;
		this.countDownloads = 0;
		this.countFailures = 0;
		this.score = 0.0d;
	}

	String getIP() {
		return ipAddr.getHostAddress();
	}

	synchronized void incrementCountDownloads() {
		this.countDownloads++;
	}

	synchronized void incrementCountFailures() {
		this.countFailures++;
	}

	@Override
	public int compareTo(ContactInfo other) {
		if (this.score > other.score)
			return 1;
		return -1;
	}

	@Override
	public String toString() {
		return "{" + username + " [ID=" + tokenID + ",DL=" + countDownloads + ",FL="
			+ countFailures + "] -> (" + getIP() + ":" + port +
			")}";
	}
}
