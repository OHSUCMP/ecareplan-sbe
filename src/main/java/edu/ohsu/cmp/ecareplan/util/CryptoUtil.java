package edu.ohsu.cmp.ecareplan.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;


public class CryptoUtil {
    private static final Logger logger = LoggerFactory.getLogger(CryptoUtil.class);

    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";

    private static final int IV_LENGTH = 16;

    public static X509Certificate readCertificate(File certFile) throws IOException, CertificateException {
        try (FileInputStream fis = new FileInputStream(certFile)) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(fis);
        }
    }

    public static PublicKey readPublicKeyFromCertificate(File certFile) throws IOException, CertificateException {
        return readCertificate(certFile).getPublicKey();
    }

    public static PrivateKey readPrivateKey(File pemFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance("RSA");
        String s = Files.readString(pemFile.toPath(), Charset.defaultCharset());
        String s2 = s.replace(PKCS8_HEADER, "")
                .replaceAll("\\s+", "")
                .replace(PKCS8_FOOTER, "");
        byte[] content = Base64.decodeBase64(s2);
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
        return factory.generatePrivate(privKeySpec);
    }

    public static byte[] randomBytes(int length) {
        byte[] b = new byte[length];
        new SecureRandom().nextBytes(b);
        return b;
    }

    public static SecretKey generateSecretKey(String password, byte[] salt) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static String encrypt(Object obj, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

        Gson gson = new GsonBuilder().create();
        byte[] encryptedBytes = cipher.doFinal(gson.toJson(obj).getBytes());

        if (iv.length != IV_LENGTH) {
            throw new InvalidParameterSpecException("IV length must be " + IV_LENGTH + " bytes");
        }

        byte[] payload = new byte[IV_LENGTH + encryptedBytes.length];
        int i = 0;
        for (byte b : iv) {
            payload[i++] = b;
        }
        for (byte b : encryptedBytes) {
            payload[i++] = b;
        }

        return Base64.encodeBase64String(payload);
    }

    public static <T> T decrypt(Class<T> clazz, String encryptedDataB64, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] payload = Base64.decodeBase64(encryptedDataB64);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedBytes = new byte[payload.length - IV_LENGTH];
        int i = 0;
        for (byte b : payload) {
            if (i < IV_LENGTH) {
                iv[i] = b;
            } else {
                encryptedBytes[i - IV_LENGTH] = b;
            }
            i++;
        }

        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] bytes = cipher.doFinal(encryptedBytes);

        Gson gson = new GsonBuilder().create();
        return gson.fromJson(new String(bytes), clazz);
    }
}
