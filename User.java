class User {
	String username;
	String password;
	int countDownloads;
	int countFailures;

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

	synchronized void incrementCountDownloads() {
		this.countDownloads++;
	}

	synchronized void incrementCountFailures() {
		this.countFailures++;
	}

	synchronized void resetCounts() {
		this.countDownloads = 0;
		this.countFailures = 0;
	}

	@Override
	public String toString() {
		return username + "[DL=" + countDownloads + ",FL=" + countFailures + "]";
	}
}
