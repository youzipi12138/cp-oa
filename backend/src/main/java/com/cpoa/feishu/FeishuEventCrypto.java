package com.cpoa.feishu;

import com.cpoa.common.BusinessException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 飞书事件回调的签名计算与内容解密。
 *
 * <p>加密规则：key = SHA256(encrypt_key)，密文 = Base64(IV(16 字节) + AES-256-CBC(data))。
 */
public final class FeishuEventCrypto {

    private FeishuEventCrypto() {
    }

    /** 飞书签名：SHA256(timestamp + nonce + encrypt_key + body)。 */
    public static String sign(String timestamp, String nonce, String encryptKey, String body) {
        String content = timestamp + nonce + encryptKey + body;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(500, "计算飞书事件签名失败");
        }
    }

    public static String decrypt(String encryptKey, String encrypted) {
        try {
            byte[] key = MessageDigest.getInstance("SHA-256")
                    .digest(encryptKey.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = Base64.getDecoder().decode(encrypted);
            if (ciphertext.length <= 16) {
                throw new BusinessException(400, "飞书事件密文格式错误");
            }
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new IvParameterSpec(Arrays.copyOfRange(ciphertext, 0, 16)));
            byte[] plain = cipher.doFinal(Arrays.copyOfRange(ciphertext, 16, ciphertext.length));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "飞书事件解密失败: " + e.getMessage());
        }
    }
}
