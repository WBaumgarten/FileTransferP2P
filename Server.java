import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    private static int uniqueID;
    private final int port;
    private boolean stillRunning;
    private static final String notif = " *** ";
    public static ArrayList<ClientThread> clientList;
    private int nextPort = 1510;
    ObjectInputStream inputStream;
    ObjectOutputStream outputStream;

    public Server(int port) {
        this.port = port;
        clientList = new ArrayList<>();
    }

    synchronized static ArrayList<ClientThread> getClientList() {
        return clientList;
    }

    public void start() {
        stillRunning = true;
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            while (stillRunning) {
                System.out.println("Server waiting for clients on port " + port);
                
                Socket socket = serverSocket.accept();

                if (!stillRunning) {
                    break;
                }

                ClientThread clientThread = new ClientThread(socket, uniqueID++);

                boolean nameExists = false;
                for (ClientThread curClient : clientList) {
                    if (curClient.getUsername().equals(clientThread.getUsername())) {
                        nameExists = true;
                        break;
                    }
                }

                if (nameExists) {
                    Message m = new Message(Message.FLAG);
                    m.setFlag(false);
                    clientThread.writeMsg(m);
                    clientThread.close();
                } else {
                    Message m = new Message(Message.FLAG);
                    m.setFlag(true);
                    m.setValue(nextPort);
                    nextPort += 10;
                            
                    System.out.println(" *** " + clientThread.getUsername() + " has joined the server. *** ");
                    clientList.add(clientThread);
                    clientThread.writeMsg(m);                    
                    clientThread.start();
                }
            }

            try {
                serverSocket.close();
                for (ClientThread curClient : clientList) {
                    curClient.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing the server and clients.");
            }

        } catch (IOException e) {
            System.err.println("Error while trying to create new server socket.");
        }
    }

    synchronized static void removeClient(int id) {
        String disconnectedClient = "";
        for (int i = 0; i < clientList.size(); i++) {
            ClientThread curClient = clientList.get(i);
            if (curClient.id == id) {
                disconnectedClient = curClient.getUsername();
                clientList.remove(i);
                break;
            }
        }
        System.out.println(notif + disconnectedClient + " has disconnected." + notif);
    }

    public static void main(String[] args) {
        int portNumber = 1500;
        switch (args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        
        Server server = new Server(portNumber);
        server.start();
    }
}
