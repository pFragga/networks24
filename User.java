/**
 * Dummy user class that is used by the Tracker remember registered users. 
 */
public class User {
	private String username, password;
	private int count_dl = 0, count_fail = 0;

	User(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public int getDownloads() {
		return this.count_dl;
	}

	public int getFailures() {
		return this.count_fail;
	}

	public synchronized void success() {
		this.count_dl++;
	}

	public synchronized void failure() {
		this.count_fail++;
	}
}
