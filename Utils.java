import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class Utils {
    
    /**
     * Encrypt given byte array using RSA encryption.
     * @param privateKey
     * @param encrypted
     * @return
     * @throws Exception 
     */
    public static byte[] rsaDecrypt(PrivateKey privateKey, byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(encrypted);
    }

    /**
     * Decrypt given byte array with RSA decryption.
     * @param publicKey
     * @param message
     * @return
     * @throws Exception 
     */
    public static byte[] rsaEncrypt(PublicKey publicKey, byte[] message) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");

        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return cipher.doFinal(message);
    }

    /**
     * Convert a double value to a byte array.
     * @param value
     * @return 
     */
    public static byte[] toByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }

    /**
     * Convert a byte array to a double value.
     * @param bytes
     * @return 
     */
    public static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    /**
     * Get this machines global IP.
     * @return 
     */
    public static String getMyIP() {
        try {
            URL whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in;
            in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
            String ip = in.readLine();
            return ip;
        } catch (IOException ex) {
            return "localhost";
        }
    }

    /**
     * Encrypt given byte array using AES encryption.
     * @param cipher
     * @param key
     * @param value
     * @param last
     * @return 
     */
    public static byte[] aesEncrypt(Cipher cipher, SecretKey key, byte[] value, boolean last) {
        try {
            byte[] encrypted;
            if (last) {
                encrypted = cipher.doFinal(value);
            } else {
                encrypted = cipher.update(value);
            }

            return encrypted;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
    
    /**
     * Decrypt given byte array using AES decryption.
     * @param cipher
     * @param key
     * @param encrypted
     * @param last
     * @return 
     */
    public static byte[] aesDecrypt(Cipher cipher, SecretKey key, byte[] encrypted, boolean last) {
        try {
            byte[] original;
            if (last) {
                original = cipher.doFinal(encrypted);
            } else {
                original = cipher.update(encrypted);
            }

            return original;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
