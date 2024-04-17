/**
 * Dummy user class that is used by the Tracker remember registered users. 
 */
public class User {
	private String username;
	private String password;
	private int countDownloads;
	private int countFailures;

	// Constructor
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.countDownloads = 0;
		this.countFailures = 0;
	}

	// Getters and setters for username and password
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	// Getters and setters for countDownloads and countFailures
	public int getCountDownloads() {
		return countDownloads;
	}

	public synchronized void setCountDownloads(int countDownloads) {
		this.countDownloads = countDownloads;
	}

	public int getCountFailures() {
		return countFailures;
	}

	public synchronized void setCountFailures(int countFailures) {
		this.countFailures = countFailures;
	}

	// Method to increment countDownloads
	public void incrementCountDownloads() {
		this.countDownloads++;
	}

	// Method to increment countFailures
	public void incrementCountFailures() {
		this.countFailures++;
	}

	// Method to reset countDownloads and countFailures
	public synchronized void resetCounts() {
		this.countDownloads = 0;
		this.countFailures = 0;
	}
}
