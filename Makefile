JAVAC = javac
JFLAGS = -g -Xlint:all -d bin -cp .

CLASSES = $(wildcard *.java)

all: classes

classes: $(CLASSES:%.java=bin/%.class)

bin/%.class: %.java
	$(JAVAC) $(JFLAGS) $<

clean:
	rm -rf *.class bin

.PHONY: all classes clean
