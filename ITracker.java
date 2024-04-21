public interface ITracker {
	public void register();
	public void login();
	public void logout();
	public void respondToNotify();
	public void reply_list();
	public void reply_details(String filename);
	public boolean checkActive(String ipAddr, int port);
}
