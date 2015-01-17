JAVAC = javac
JFLAGS = -g -d . -classpath .

default: all

all: Client Server

Client: Client.java Utils.java
	$(JAVAC) $(JFLAGS) Client.java Utils.java

Server: Server.java Utils.java
	$(JAVAC) $(JFLAGS) Server.java Utils.java

clean:
	rm -f *.class


