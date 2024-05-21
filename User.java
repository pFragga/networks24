class User {
	String username;
	String password;

	User(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public String toString() {
		return "[User: " + username + "]";
	}
}
