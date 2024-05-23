JAVAC = javac
JFLAGS = -g -Xlint:all -d bin -cp .

CLASSES = $(wildcard *.java)

all: classes

classes: $(CLASSES:%.java=bin/%.class)

bin/%.class: %.java
	$(JAVAC) $(JFLAGS) $<

clean_parts:
	rm -rf shared_directory_*/*.part

clean: clean_parts
	rm -rf *.class bin/*.class

.PHONY: all classes clean_parts clean
