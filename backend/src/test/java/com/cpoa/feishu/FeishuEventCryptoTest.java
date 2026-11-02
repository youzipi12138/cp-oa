package com.cpoa.feishu;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeishuEventCryptoTest {

    @Test
    void signCalculatesSha256OfTimestampNonceKeyAndBody() {
        // SHA256("abc")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                FeishuEventCrypto.sign("a", "b", "c", ""));
    }

    @Test
    void decryptReadsFeishuEncryptedPayload() throws Exception {
        String encryptKey = "test-encrypt-key";
        String plain = "{\"schema\":\"2.0\",\"header\":{\"event_type\":\"contact.user.created_v3\"}}";

        String decrypted = FeishuEventCrypto.decrypt(encryptKey, encrypt(encryptKey, plain));

        assertEquals(plain, decrypted);
    }

    /** 按飞书规则加密：Base64(IV + AES-256-CBC(SHA256(encrypt_key), plain))。 */
    private static String encrypt(String encryptKey, String plain) throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(encryptKey.getBytes(StandardCharsets.UTF_8));
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] data = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[iv.length + data.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(data, 0, combined, iv.length, data.length);
        return Base64.getEncoder().encodeToString(combined);
    }
}
