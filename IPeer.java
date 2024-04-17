import java.util.List;
import java.util.ArrayList;

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
