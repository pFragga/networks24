JAVAC = javac
JFLAGS = -g -Xlint:all -d bin -cp .

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
	rm -r bin

.PHONY: all clean
