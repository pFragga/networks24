import java.net.InetAddress;

class ContactInfo {
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

	@Override
	public String toString() {
		return "{" + username + " [ID=" + tokenID + ",DL=" + countDownloads + ",FL="
			+ countFailures + "] -> (" + ipAddr.getHostAddress() + ":" + port +
			")}";
	}
}
