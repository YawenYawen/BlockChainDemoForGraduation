package testVersion;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.LinkedList;
import java.util.List;

public class EncryptUtils {

    private static final String SIGN_ALGORITHMS = "SHA1WithRSA";

    /**
     * 随机生成密钥对
     */
    public static String[] genKeyPair(String filePath) {
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
        KeyPairGenerator keyPairGen = null;
        try {
            keyPairGen = KeyPairGenerator.getInstance("RSA");

            // 初始化密钥对生成器，密钥大小为96-1024位
            keyPairGen.initialize(512, new SecureRandom());
            // 生成一个密钥对，保存在keyPair中
            KeyPair keyPair = keyPairGen.generateKeyPair();
            // 得到私钥
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();//默认pkcs8
            // 得到公钥
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();//默认X509
            // 得到公钥字符串
            String publicKeyString = new String(Base64.encodeBase64(publicKey.getEncoded()));
            // 得到私钥字符串
            String privateKeyString = new String(Base64.encodeBase64(privateKey.getEncoded()));

            // 将密钥对写入到文件
//            FileWriter pubfw = new FileWriter(filePath + "/publicKey.keystore");
//            FileWriter prifw = new FileWriter(filePath + "/privateKey.keystore");
//            BufferedWriter pubbw = new BufferedWriter(pubfw);
//            BufferedWriter pribw = new BufferedWriter(prifw);
//            pubbw.write(publicKeyString);
//            pribw.write(privateKeyString);
//            pubbw.flush();
//            pubbw.close();
//            pubfw.close();
//            pribw.flush();
//            pribw.close();
//            prifw.close();
//
            return new String[]{privateKeyString, publicKeyString};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 私钥解密
     *
     * @param publicKey     base64形式的公钥
     * @param encryptedData base64形式加密数据
     * @return 普通形式明文数据
     */
    public static String decryptByPrivateKey(String publicKey, String encryptedData) {
        try {
            //为了解决长度问题故分段加密，分段解密
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.decodeBase64(publicKey.getBytes())));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(Base64.decodeBase64(encryptedData.getBytes())));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 私钥加密过程
     *
     * @param privateKeyStr base64形式的私钥
     * @param plainTextData 普通形式明文数据
     * @return base64形式密文
     * @throws Exception 加密过程中的异常信息
     */
    public static String encryptByPrivateKey(String privateKeyStr, String plainTextData) throws Exception {
        Cipher cipher = null;
        try {
            // 使用默认RSA
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyStr.getBytes())));
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return new String(Base64.encodeBase64(cipher.doFinal(plainTextData.getBytes())));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 公钥解密
     *
     * @param publicKey     base64形式的公钥
     * @param encryptedData base64形式密文
     * @return 普通形式明文数据
     */
    public static String decryptByPublicKey(String publicKey, String encryptedData) {
        try {
            //为了解决长度问题故分段加密，分段解密
            KeyFactory mykeyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = mykeyFactory.generatePublic(new X509EncodedKeySpec(Base64.decodeBase64(publicKey.getBytes())));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, pubKey);
            return new String(cipher.doFinal(Base64.decodeBase64(encryptedData.getBytes())));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 公钥加密过程
     *
     * @param publicKey     base64形式的公钥
     * @param plainTextData 普通形式明文数据
     * @return base64形式密文
     * @throws Exception 加密过程中的异常信息
     */
    public static String encryptByPublicKey(String publicKey, String plainTextData) throws Exception {
        Cipher cipher = null;
        try {
            // 使用默认RSA
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, loadPublicKeyByStr(publicKey));
            return new String(Base64.encodeBase64(cipher.doFinal((plainTextData.getBytes()))));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 从文件中输入流中加载公钥
     *
     * @param path 公钥路径
     * @throws Exception 加载公钥时产生的异常
     */
    public static String loadDataFromFile(String path) throws Exception {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String readLine = null;
            StringBuilder sb = new StringBuilder();
            while ((readLine = br.readLine()) != null) {
                sb.append(readLine);
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            throw new Exception("公钥数据流读取错误");
        } catch (NullPointerException e) {
            throw new Exception("公钥输入流为空");
        }
    }

    /**
     * 从文件中输入流中加载公钥
     *
     * @param path 公钥路径
     * @throws Exception 加载公钥时产生的异常
     */
    public static List<String> loadDatasFromFile(String path) throws Exception {
        List<String> keys = new LinkedList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = null;
            while ((line = br.readLine()) != null) {
                keys.add(line);
            }
            br.close();
            return keys;
        } catch (IOException e) {
            throw new Exception("公钥数据流读取错误");
        } catch (NullPointerException e) {
            throw new Exception("公钥输入流为空");
        }
    }

    /**
     * 从文件中加载私钥
     *
     * @param path 私钥文件路径
     * @return 是否成功
     * @throws Exception
     */
    public static String loadPrivateKeyByFile(String path) throws Exception {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String readLine = null;
            StringBuilder sb = new StringBuilder();
            while ((readLine = br.readLine()) != null) {
                sb.append(readLine);
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            throw new Exception("私钥数据读取错误");
        } catch (NullPointerException e) {
            throw new Exception("私钥输入流为空");
        }
    }

    /**
     * 从字符串中加载公钥
     *
     * @param publicKeyStr base64形式的公钥
     * @throws Exception 加载公钥时产生的异常
     */
    private static RSAPublicKey loadPublicKeyByStr(String publicKeyStr) throws Exception {
        try {
            byte[] buffer = Base64.decodeBase64(publicKeyStr.getBytes());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("无此算法");
        } catch (InvalidKeySpecException e) {
            throw new Exception("公钥非法");
        } catch (NullPointerException e) {
            throw new Exception("公钥数据为空");
        }
    }

    public static void main(String args[]) throws Exception {
        String pub = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCYoF76kJWDkt6DQ6TcQ6Qhyp9FaKp3ad3jsDLXHywFZmDJTJrk1qTZTzSjQhHdOv2AmEVZM3/sdgCKqM+ATFqv0KeJg1CvihGCkA91YkRWC5kJkanC12HKVe+6vumO75isokuTxtZA19PZK89JlPp2t3ZdUEASLf5tbthRxFACawIDAQAB";
        String pri = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJigXvqQlYOS3oNDpNxDpCHKn0Voqndp3eOwMtcfLAVmYMlMmuTWpNlPNKNCEd06/YCYRVkzf+x2AIqoz4BMWq/Qp4mDUK+KEYKQD3ViRFYLmQmRqcLXYcpV77q+6Y7vmKyiS5PG1kDX09krz0mU+na3dl1QQBIt/m1u2FHEUAJrAgMBAAECgYAQw/UK8xzpYhW9N44BgBCkgZxzRGcNTPzJyQ4coVj1kuRo2FhfcNCU6mVu1ZAezB8SpCdiAA06TVAL6zU386s2XzOadyxeJFYQE09Eg0oBTJUoDG/TdjZz7vT/rYkeI6P3gaiVHyPPl62qHXrLCndZHSEGiovkrodrfMC+dExCQQJBAN0EhVPAKVDJtkPmDhjp25az/BNuyVg0iEvHqfVe5fVZ4zJjYQ8BYeHyI7CHww48PNwN/A/xfvltfK9Q+VY880sCQQCwyK/PBhN7BkftmBwOaUlBbCj+7aJWLbcN/9wRT1tSbnff617t/hhWdHBYyfp62h7QaiJTCCg5bzFkGC0LcJlhAkB9PAWMOjNu+o0TljTguwpZL20jWcAvAb1FK0LLrVUiHCfXsmYZyY+8JqdAbP0CgPF/Q1FTe2SWUwq2kGxoCCtXAkBsEvQvytYj8q4MWV5ljQYkwbu6RYplxuLO4yklR+9bUH9mW90X/6vfRqXcEfZYHnqSDSBYxHbazdhTCdiX0slhAkEA0HnOSjCEmgHbU9/sCBaOTxPe8Cc8irXvRKpR1zW4PkVPEFh74EzGjLJJWyhBcMPC8aBJoOWUpofLLTuIXDzxhQ==";
        String encryptData = encryptByPrivateKey(pri, "zywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzywzyw");
        System.out.println(encryptData);
        String data = decryptByPublicKey(pub, encryptData);
        System.out.println(data);

        encryptData = encryptByPublicKey(pub, "12345678912345678912345678912345678912345678912345678912345678912345678912345678912345678912345678912345678912345678912345678912345678912345678912345678912345678912345679123456789");
        System.out.println(encryptData);
        data = decryptByPrivateKey(pri, encryptData);
        System.out.println(data);
    }
}
