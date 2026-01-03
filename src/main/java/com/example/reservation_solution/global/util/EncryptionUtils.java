package com.example.reservation_solution.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 전화번호 등 개인정보 암호화/복호화 유틸리티
 * AES-256 알고리즘 사용
 */
@Component
public class EncryptionUtils {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    private final SecretKeySpec secretKey;
    private final IvParameterSpec ivParameterSpec;

    public EncryptionUtils(@Value("${encryption.secret-key:ticket-form-default-secret-key-32chars!!}") String secretKey) {
        try {
            // 비밀키를 32바이트로 정규화 (AES-256 요구사항)
            byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 32);

            this.secretKey = new SecretKeySpec(key, KEY_ALGORITHM);

            // IV (Initialization Vector) 생성 - 16바이트
            byte[] iv = Arrays.copyOf(key, 16);
            this.ivParameterSpec = new IvParameterSpec(iv);
        } catch (Exception e) {
            throw new RuntimeException("암호화 초기화 실패", e);
        }
    }

    /**
     * 전화번호 암호화
     * @param plainText 평문 전화번호
     * @return Base64 인코딩된 암호문
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("암호화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 전화번호 복호화
     * @param encryptedText Base64 인코딩된 암호문
     * @return 평문 전화번호
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.trim().isEmpty()) {
            return encryptedText;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("복호화 실패: " + e.getMessage(), e);
        }
    }
}
