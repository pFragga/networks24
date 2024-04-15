JAVA = java -cp bin
JAVAC = javac -d bin -cp .

SRCS = Peer.java Tracker.java TrackerThread.java Message.java User.java
CLASSES := $(SRCS:%.java=bin/%.class)

all: $(CLASSES)

bin/%.class: %.java
	$(JAVAC) $^

tracker: bin/Tracker.class
	$(JAVA) Tracker

peer: bin/Peer.class
	$(JAVA) Peer

clean:
	rm -f *.class
	rm -r bin

.PHONY: all tracker peer clean
