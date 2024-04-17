//import java.util.List;
//import java.util.ArrayList;

public interface ITracker {
	/* TODO don't save Peers here, consider creating a User class */
	//List<Peer> registeredPeersInfo = new ArrayList<>();
	//List<Peer> activePeersInfo = new ArrayList<>();
	//List<String> allFilenames = new ArrayList<>();
	//Map<String, Integer> filenamesToTokenIDs = null;

	public void register();
	public void login();
	public void logout();
	public void respondToNotify();
	public void reply_list();
	public void reply_details();
}
