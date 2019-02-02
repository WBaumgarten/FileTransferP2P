import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientThread extends Thread {

    Socket socket;
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;
    int id;
    String username;
    Message msg;

    public ClientThread(Socket socket, int uniqueID) {
        id = uniqueID;
        this.socket = socket;
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            Message m = (Message) inputStream.readObject();
            username = m.getMessage();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Exception creating new Input/Output streams: " + e);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void run() {
        boolean stillRunning = true;

        while (stillRunning) {
            try {
                msg = (Message) inputStream.readObject();
            } catch (IOException e) {
                break;
            } catch (ClassNotFoundException e) {
                System.err.println(username + "Class not found: " + e);
                break;
            }

            switch (msg.getType()) {
                case Message.SEARCH:
                    String stringToSearch = msg.getMessage();
                    System.out.println("File '" + stringToSearch + "' is being searched for by " + msg.getUsername());
                    Message m = new Message(Message.FIND);
                    m.setUsername(msg.getUsername());
                    m.setMesssage(stringToSearch);
                    // send message to every other client of the file being searched for.
                    for (ClientThread ct : Server.clientList) {
                        if (!ct.username.equals(this.username)) {
                            ct.writeMsg(m);
                        }
                    }
                    break;
                case Message.SEARCHRESULT:
                    // pass on search results from sending client to receiving client.
                    for (ClientThread ct : Server.clientList) {
                        if (ct.username.equals(msg.getUsername())) {
                            System.out.println(msg.getUsername() + " got file " + msg.getMessage());
                            ct.writeMsg(msg);
                            break;
                        }
                    }
                    break;
                case Message.REQUESTDOWNLOAD:
                    boolean found = false;
                    for (ClientThread ct : Server.clientList) {
                        if (ct.username.equals(msg.getUsername())) {
                            System.out.println(msg.getUsername2() + " requests download " + msg.getMessage() + " from " + msg.getUsername());
                            ct.writeMsg(msg);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Message flag = new Message(Message.DOWNLOAD);
                        flag.setFlag(found);
                        writeMsg(flag);
                    }
                    break;
                case Message.DOWNLOAD:
                    for (ClientThread ct : Server.clientList) {
                        if (ct.username.equals(msg.getUsername())) {
                            System.out.println("Download started.");
                            ct.writeMsg(msg);
                            break;
                        }
                    }
                    break;
            }

        }
        Server.removeClient(id);
        close();
    }

    public boolean writeMsg(Message msg) {
        if (!socket.isConnected()) {
            close();
            return false;
        }

        try {
            outputStream.writeObject(msg);
        } catch (IOException e) {
            System.err.println("Error sending message to " + username + "\n" + e.toString());
        }
        return true;
    }

    public void close() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }

        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

}
