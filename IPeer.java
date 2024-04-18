import java.util.ArrayList;
import java.util.List;

public interface IPeer {
	List<String> shared_directory = new ArrayList<>();

	public void list();
	public void details();
	public void checkActive();
	public void simpleDownload();
	public void notifyTracker();
	public void inform();
	public void register();
	public void login();
	public void logout();
}
