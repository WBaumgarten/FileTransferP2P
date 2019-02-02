import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Client {

    private String serverIP;
    private int serverPort;
    private int sendPort;

    private Socket socketToServer; // connection to server
    private static Socket socketUpload; // conncetion to client    
    private static Socket socketDownload;
    private static ServerSocket socketAccept;
    private static ListenFromServer listenerFromServer;
    private ObjectOutputStream serverOut;
    private ObjectInputStream serverIn;
    private final String username;
    private ArrayList<FileEntry> publicFiles = new ArrayList<>();
    private ArrayList<String> resultFiles = new ArrayList<>();
    private boolean upPause = false;
    private boolean downPause = false;
    private double curRandomKey;
    private static PublicKey publicKey;
    private static PublicKey publicKeySend;
    private PrivateKey privateKey;
    private String destFilePath;
    private static String secretKeySendString;
    private static String secretKeyRecvString;
    private static SecretKey aesKeySend;
    private static SecretKey aesKeyRecv;
    private static final String ENCRYPTION_ALGO = "AES";
    private static boolean isUploading = false;
    private static Cipher sendCipher;
    private static Cipher recvCipher;

    public Client(String serverIP, int serverPort, String username) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.username = username;
    }

    /**
     * Create the public and private key for RSA encryption.
     */
    public void generateKeyPairs() {
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.genKeyPair();
            publicKey = kp.getPublic();
            privateKey = kp.getPrivate();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Start the client.
     *
     * @return
     */
    public boolean start() {
        try {
            socketToServer = new Socket(serverIP, serverPort);
        } catch (IOException e) {
            System.out.println("Error connecting to server.");
            return false;
        }

        try {
            serverIn = new ObjectInputStream(socketToServer.getInputStream());
            serverOut = new ObjectOutputStream(socketToServer.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error opening streams.");
            return false;
        }

        try {
            Message m = new Message(Message.TEXT);
            m.setMesssage(username);
            serverOut.writeObject(m);
        } catch (IOException e) {
            disconnect();
            return false;
        }

        return true;
    }

    /**
     * Send a message as a Message object
     *
     * @param m
     */
    public void sendMessage(Message m) {
        try {
            serverOut.writeObject(m);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Read in the initial message from the server to initialize the port
     * number.
     *
     * @return
     */
    public boolean readStartMessage() {
        try {
            Message m = (Message) serverIn.readObject();
            if (m.getType() == Message.FLAG) {
                sendPort = m.getValue();
                return m.getFlag();
            } else {
                return false;
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    /**
     *
     * When something goes wrong Close the Input/Output streams and disconnect
     */
    public void disconnect() {
        sendMessage(new Message(Message.LOGOUT));
        try {
            if (serverIn != null) {
                serverIn.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        try {
            if (serverOut != null) {
                serverOut.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        try {
            if (socketToServer != null) {
                socketToServer.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
        publicFiles.clear();
    }

    /**
     * Start listening for messages from the server.
     */
    public void startListeningFromServer() {
        listenerFromServer = new ListenFromServer();
        listenerFromServer.start();
    }

    /**
     * Stop listening to messages from the server.
     */
    public void stopListeningFromServer() {
        listenerFromServer.stop();
    }

    class SendFile extends Thread {

        String filePath;
        String message;

        SendFile(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            FileInputStream fis = null;
            try {
                socketUpload = socketAccept.accept();
                System.out.println("Socket upload accepted.");
                for (FileEntry publicFile : publicFiles) {
                    if (publicFile.getFileName().equals(message)) {
                        isUploading = true;
                        filePath = publicFile.getPath();
                    }
                }
                sendCipher = Cipher.getInstance("AES");
                sendCipher.init(Cipher.ENCRYPT_MODE, aesKeySend);
                upPause = false;
                ClientUI.btnUpPause.setText("Pause");
                File file = new File(filePath);
                fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                ObjectOutputStream os = new ObjectOutputStream(socketUpload.getOutputStream());
                byte[] contents;
                int fileLength = (int) file.length();
                int sendLength = fileLength;
                ClientUI.progressBarUp.setMaximum(sendLength);
                os.writeInt(sendLength);

                int current = 0;
                int size;
                ClientUI.btnUpPause.setEnabled(true);
                while (current != sendLength) {
                    while (upPause) {                        
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    size = 1024;
                    if (sendLength - current >= size) {
                        current += size;
                    } else {
                        size = (int) (sendLength - current);
                        current = sendLength;
                    }

                    contents = new byte[size];
                    bis.read(contents, 0, size);
                    byte[] enryptedBytes = Utils.aesEncrypt(sendCipher, aesKeySend, contents, true);
                    os.writeObject(enryptedBytes);
                    ClientUI.progressBarUp.setValue((int) current);
                }
                if (current < sendLength) {
                    ClientUI.displayToClient("Upload stoppped; connection lost.");
                }

                bis.close();
                os.flush();
                os.close();

            } catch (FileNotFoundException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException e) {
                ClientUI.displayToClient("Upload stoppped; connection lost.");
                isUploading = false;
            } finally {
                try {
                    socketUpload.close();
                    socketAccept.close();
                    isUploading = false;
                    ClientUI.btnUpPause.setEnabled(false);
                    sendCipher = null;
                    this.interrupt();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    class ReceiveFile extends Thread {

        @Override
        public void run() {
            try {
                recvCipher = Cipher.getInstance("AES");
                recvCipher.init(Cipher.DECRYPT_MODE, aesKeyRecv);
                downPause = false;
                ClientUI.btnDownPause.setText("Pause");
                ClientUI.btnDownload.setEnabled(false);
                ObjectInputStream inputStream = new ObjectInputStream(socketDownload.getInputStream());
                int fileSize = inputStream.readInt();
                ClientUI.progressBarDown.setMaximum(fileSize);

                FileOutputStream fos = new FileOutputStream(destFilePath);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                //No of bytes read in one read() call
                int bytesRead = 0;
                long current = 0;
                long startTime = System.nanoTime();
                ClientUI.btnDownPause.setEnabled(true);

                while (current < fileSize) {
                    while (downPause) {                        
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    byte[] encryptedRecv = (byte[]) inputStream.readObject();
                    bytesRead = encryptedRecv.length;
                    byte[] decryptedRecv = Utils.aesDecrypt(recvCipher, aesKeyRecv, encryptedRecv, true);
                    current += decryptedRecv.length;
                    ClientUI.progressBarDown.setValue((int) current);
                    bos.write(decryptedRecv, 0, decryptedRecv.length);
                }

                bos.close();
                fos.close();
                if (current >= fileSize) {
                    System.out.println("Received in: " + (System.nanoTime() - startTime) / 1000000 + " ms");
                    startTime = System.nanoTime();
                    ClientUI.btnDownPause.setEnabled(false);
                    ClientUI.displayToClient("File saved successfully!");
                } else {
                    ClientUI.displayToClient("The connection was closed during download.");
                    File file = new File(destFilePath);
                    if (file.delete()) {
                        System.out.println("File deleted successfully");
                    } else {
                        System.out.println("Failed to delete the file");
                    }

                }
            } catch (IOException e) {
                ClientUI.displayToClient("The connection was closed during download.");
                File file = new File(destFilePath);
                if (file.delete()) {
                    System.out.println("File deleted successfully");
                } else {
                    System.out.println("Failed to delete the file");
                }
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | ClassNotFoundException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    ClientUI.btnDownPause.setEnabled(false);
                    ClientUI.btnDownload.setEnabled(true);
                    socketDownload.close();
                    this.interrupt();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    class ListenFromServer extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    Message m = (Message) serverIn.readObject();
                    switch (m.getType()) {
                        case Message.FIND:
                            System.out.println("Find: " + m.getMessage());
                            findFiles(m);
                            break;
                        case Message.SEARCHRESULT:
                            for (String file : m.getFiles()) {
                                resultFiles.add(m.getMessage() + ": " + file);
                            }
                            Collections.sort(resultFiles);
                            ClientUI.updateList(resultFiles);
                            System.out.println(resultFiles);
                            break;
                        case Message.REQUESTDOWNLOAD:
                            if (!isUploading) {
                                sendDownloadReply(m);
                                socketAccept = new ServerSocket(sendPort);
                                System.out.println("Socket upload started");
                                SendFile t = new SendFile(m.getMessage());
                                t.start();

                            } else {
                                ClientUI.displayToClient("Only one upload at a time.");
                                Message downloadReply = new Message(Message.DOWNLOAD);
                                downloadReply.setFlag(false);
                                downloadReply.setUsername(m.getUsername2());
                                sendMessage(downloadReply);
                            }
                            break;
                        case Message.DOWNLOAD:
                            if (m.getFlag()) {
                                System.out.println("Start downloading");
                                byte[] verified = Utils.rsaDecrypt(privateKey, m.getEncrypted());
                                double verificationKey = Utils.toDouble(verified);
                                if (verificationKey == curRandomKey) {
                                    secretKeyRecvString = new String(Utils.rsaDecrypt(privateKey, m.getSecretKey()));
                                    byte[] aesBuf = Base64.getDecoder().decode(secretKeyRecvString);
                                    aesKeyRecv = new SecretKeySpec(aesBuf, ENCRYPTION_ALGO);
                                    socketDownload = new Socket(m.getIP(), m.getPort());
                                    new ReceiveFile().start();
                                } else {
                                    System.out.println("Someone was trying to be malicious.");
                                }
                            } else {
                                ClientUI.displayToClient("This file is not available for uploading.");
                            }
                            break;
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    System.out.println("You have disconneted");
                } catch (Exception ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        private void sendDownloadReply(Message m) throws Exception, NoSuchAlgorithmException {
            Message downloadReply = new Message(Message.DOWNLOAD);
            //downloadReply.setIP(Utils.getMyIP());
            downloadReply.setIP("localhost");
	    downloadReply.setPort(sendPort);
            downloadReply.setUsername(m.getUsername2());
            downloadReply.setFlag(true);
            publicKeySend = m.getPublicKey();
            byte[] signed = Utils.rsaEncrypt(m.getPublicKey(), Utils.toByteArray(m.getRandomKey()));
            downloadReply.setEncrypted(signed);
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = new SecureRandom();
            int keyBitSize = 128;
            keygen.init(keyBitSize, secureRandom);
            aesKeySend = keygen.generateKey();
            secretKeySendString = Base64.getEncoder().encodeToString(aesKeySend.getEncoded());
            byte[] encryptedAesKey = Utils.rsaEncrypt(publicKeySend, secretKeySendString.getBytes());

            downloadReply.setSecretKey(encryptedAesKey);
            sendMessage(downloadReply);
        }

        private void findFiles(Message m) {
            ArrayList<String> filesFound = new ArrayList<>();
            for (FileEntry file : publicFiles) {
                if (file.getFileName().contains(m.getMessage())) {
                    filesFound.add(file.getFileName());
                }
            }
            if (filesFound.size() > 0) {
                Message ret = new Message(Message.SEARCHRESULT);
                ret.setMesssage(username);
                ret.setFiles(filesFound);
                ret.setUsername(m.getUsername());
                sendMessage(ret);
            }
        }
    }

    //utils was here
    public String getDestFilePath() {
        return destFilePath;
    }

    public void setDestFilePath(String destFilePath) {
        this.destFilePath = destFilePath;
    }

    public boolean isUpPause() {
        return upPause;
    }

    public void setUpPause(boolean upPause) {
        this.upPause = upPause;
    }

    public boolean isDownPause() {
        return downPause;
    }

    public void setDownPause(boolean downPause) {
        this.downPause = downPause;
    }

    public ArrayList<String> getResultFiles() {
        return resultFiles;
    }

    public void setResultFiles(ArrayList<String> resultFiles) {
        this.resultFiles = resultFiles;
    }

    public void clearResults() {
        resultFiles = new ArrayList<>();
        ClientUI.updateList(resultFiles);
    }

    public boolean isConnect() {
        return socketToServer.isConnected();
    }

    public ArrayList<FileEntry> getFiles() {
        return publicFiles;
    }

    public void addFile(FileEntry file) {
        publicFiles.add(file);
        System.out.println("success");
    }

    public String getUsername() {
        return username;
    }

    public double getCurRandomKey() {
        return curRandomKey;
    }

    public void setCurRandomKey(double curRandomKey) {
        this.curRandomKey = curRandomKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

}
