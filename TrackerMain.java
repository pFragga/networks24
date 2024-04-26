class TrackerMain {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: [java] Tracker <port>");
			System.exit(1);
		}

		Tracker tracker = new Tracker();
		tracker.listen(Integer.parseInt(args[0]));
	}
}
