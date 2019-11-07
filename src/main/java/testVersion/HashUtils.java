package testVersion;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Formatter;


public class HashUtils {

    private static final String KEYGEN_SPEC = "PBKDF2WithHmacSHA1";
    private static final int ITERATIONS = 32768;

    /***
     *  利用Apache的工具类实现SHA-256加密,两次
     * @param str 加密后的报文
     * @return
     */
    public static String applySha256ByApacheTwice(String str) {
        MessageDigest messageDigest;
        String encdeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes(), 0, str.length());
            byte[] first = messageDigest.digest();
            return toHexString(messageDigest.digest(first));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encdeStr;
    }

    /***
     *  利用Apache的工具类实现SHA-256加密,两次
     * @param str 加密后的报文
     * @return
     */
    public static byte[] applySha256ByApacheTwice(byte[] str) {
        MessageDigest messageDigest;
        byte[] encdeStr = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str, 0, str.length);
            byte[] first = messageDigest.digest();
            return messageDigest.digest(first);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encdeStr;
    }

    /**
     * byte数组 -> 十六进制字符串
     *
     * @param bytes
     * @return
     */
    public static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    /**
     * 十六进制字符串 -> byte数组
     *
     * @param hex
     * @return
     */
    public static byte[] hexStringToByteArray(String hex) {
        byte[] b = new byte[hex.length() / 2];
        for (int i = 0; i < b.length; i++) {
            String subStr = hex.substring(i * 2, i * 2 + 2);
            b[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return b;
    }

    /**
     * 使用AES对称加密
     */
    public static byte[] encryptByAES(String data, String password, String salt) throws Exception {
        byte[] fullKey = keygen(password.toCharArray(), salt.getBytes());
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(fullKey, "AES"));
        byte[] encrypt = cipher.doFinal(data.getBytes());
        return encrypt;
    }

    /**
     * 使用AES对称解密
     */
    public static byte[] decryptByAES(byte[] encryptedText, String password, String salt) throws Exception {
        byte[] fullKey = keygen(password.toCharArray(), salt.getBytes());
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(fullKey, "AES"));
        byte[] decrypt = cipher.doFinal(encryptedText);
        return decrypt;
    }

    /**
     * 产生AES对称密钥
     *
     * @param password
     * @param salt
     * @return
     */
    private static byte[] keygen(char[] password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEYGEN_SPEC);
        KeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 16 * 8);
        SecretKey tmp = factory.generateSecret(spec);
        byte[] fullKey = tmp.getEncoded();
//        System.out.println("fullkey:::" + toHexString(fullKey));
        return fullKey;
    }
}
