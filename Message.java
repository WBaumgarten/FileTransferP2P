import java.io.Serializable;
import java.util.ArrayList;
import java.security.PublicKey;

public class Message implements Serializable {

    public static final int SEARCH = 0, FLAG = 1, TEXT = 2, LOGOUT = 3,
            FIND = 4, SEARCHRESULT = 5, DOWNLOAD = 6, VALUE = 7,
            PAUSE = 8, REQUESTDOWNLOAD = 9;
    private final int type;
    private String message;
    private String username;
    private String username2;
    private String IP;
    private int port;
    private int value;
    private double randomKey;
    private PublicKey publicKey;
    private byte[] encrypted;    
    private byte[] secretKey;
    private boolean flag;
    private ArrayList<String> files;

    public byte[] getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(byte[] secretKey) {
        this.secretKey = secretKey;
    }
    

    public String getUsername2() {
        return username2;
    }

    public void setUsername2(String username2) {
        this.username2 = username2;
    }

    public byte[] getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(byte[] encrypted) {
        this.encrypted = encrypted;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public double getRandomKey() {
        return randomKey;
    }

    public void setRandomKey(double randomKey) {
        this.randomKey = randomKey;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    

    Message(int type) {
        this.type = type;
    }

    int getType() {
        return type;
    }

    public void setMesssage(String s) {
        this.message = s;
    }

    public String getMessage() {
        return message;
    }

    public boolean getFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public ArrayList<String> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<String> files) {
        this.files = files;
    }

}
