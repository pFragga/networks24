class PeerMain {
	public static void main(String[] args) {
		if (args.length != 3) {
			System.err.println("usage: [java] Peer <tracker host> <tracker port> <dir>");
			System.exit(1);
		}

		/* TODO: add sanity checks for args */

		Peer peer = new Peer(args[0], Integer.parseInt(args[1]), args[2]);
		peer.begin();
	}
}
