JAVAC = javac
JFLAGS = -g -d bin -cp .

CLASSES = \
		  IPeer.java \
		  ITracker.java \
		  Message.java \
		  Peer.java \
		  Tracker.java \
		  TrackerThread.java \
		  User.java \


all: classes

classes: $(CLASSES:%.java=bin/%.class)

bin/%.class: %.java
	$(JAVAC) $(JFLAGS) $*.java

clean:
	rm -f *.class

.PHONY: all clean
