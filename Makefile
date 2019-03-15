.PHONY: all run clean

all: compile

compile: bin
	javac -Xlint:unchecked -cp lib/junit-4.12.jar -d bin src/*.java src/*/*.java

bin:
	mkdir bin

run: compile
	java -cp lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar:bin -Xmx20G Main

debugrun: compile
	java -cp lib/junit-4.12.jar:lib/hamcrest-core-1.3.jar:bin -Xmx20G -enableassertions Main

clean:
	rm -rf bin results
