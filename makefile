JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
        FileEntry.java \
        Message.java \
        Utils.java \
		ClientThread.java \
        Server.java \
        Client.java \
		ClientUI.java		

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
