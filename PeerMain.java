class PeerMain {
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("usage: [java] Peer <tracker host> <tracker port>");
			System.exit(1);
		}

		Peer peer = new Peer(args[0], Integer.parseInt(args[1]));
		peer.begin();
	}
}
