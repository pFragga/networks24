class User {
	String username;
	String password;
	int countDownloads;
	int countFailures;

	// Constructor
	User(String username, String password) {
		this.username = username;
		this.password = password;
		this.countDownloads = 0;
		this.countFailures = 0;
	}

	synchronized void setCountDownloads(int countDownloads) {
		this.countDownloads = countDownloads;
	}

	synchronized void setCountFailures(int countFailures) {
		this.countFailures = countFailures;
	}

	// Method to increment countDownloads
	void incrementCountDownloads() {
		this.countDownloads++;
	}

	// Method to increment countFailures
	void incrementCountFailures() {
		this.countFailures++;
	}

	// Method to reset countDownloads and countFailures
	synchronized void resetCounts() {
		this.countDownloads = 0;
		this.countFailures = 0;
	}

	@Override
	public String toString() {
		return username + "[DL=" + countDownloads + ",FL=" + countFailures + "]";
	}
}
