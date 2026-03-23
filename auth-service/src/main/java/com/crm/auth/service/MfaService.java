package com.crm.auth.service;

import com.crm.auth.entity.MfaConfig;
import com.crm.auth.repository.MfaConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private final MfaConfigRepository mfaConfigRepository;

    private static final int TOTP_PERIOD = 30;
    private static final int TOTP_DIGITS = 6;
    private static final int TOTP_WINDOW = 1; // allow ±1 period

    /**
     * Check if user has MFA enabled.
     */
    public boolean isMfaEnabled(UUID userId, String tenantId) {
        List<MfaConfig> configs = mfaConfigRepository.findByUserIdAndTenantId(userId, tenantId);
        return configs.stream().anyMatch(MfaConfig::isEnabled);
    }

    /**
     * Verify a TOTP code for the user.
     */
    public boolean verifyCode(UUID userId, String tenantId, String code) {
        MfaConfig config = mfaConfigRepository
                .findByUserIdAndMfaTypeAndTenantId(userId, "TOTP", tenantId)
                .orElse(null);

        if (config == null || !config.isEnabled()) {
            return false;
        }

        // Try TOTP verification
        if (verifyTotp(config.getSecretKey(), code)) {
            config.setLastUsedAt(LocalDateTime.now());
            mfaConfigRepository.save(config);
            return true;
        }

        // Try backup codes
        if (config.getBackupCodes() != null && !config.getBackupCodes().isEmpty()) {
            List<String> codes = new java.util.ArrayList<>(
                    Arrays.asList(config.getBackupCodes().split(",")));
            if (codes.remove(code.trim())) {
                config.setBackupCodes(String.join(",", codes));
                config.setLastUsedAt(LocalDateTime.now());
                mfaConfigRepository.save(config);
                log.info("MFA backup code used for user {}", userId);
                return true;
            }
        }

        return false;
    }

    /**
     * Get the TOTP secret URI for QR code generation.
     */
    public String getTotpUri(UUID userId, String tenantId, String email) {
        MfaConfig config = mfaConfigRepository
                .findByUserIdAndMfaTypeAndTenantId(userId, "TOTP", tenantId)
                .orElse(null);
        if (config == null || config.getSecretKey() == null) return null;

        String base32Secret = base64ToBase32(config.getSecretKey());
        return String.format("otpauth://totp/CRM:%s?secret=%s&issuer=CRM&digits=%d&period=%d",
                email, base32Secret, TOTP_DIGITS, TOTP_PERIOD);
    }

    private boolean verifyTotp(String base64Secret, String code) {
        try {
            byte[] key = Base64.getDecoder().decode(base64Secret);
            long currentInterval = Instant.now().getEpochSecond() / TOTP_PERIOD;

            for (int i = -TOTP_WINDOW; i <= TOTP_WINDOW; i++) {
                String generated = generateTotp(key, currentInterval + i);
                if (generated.equals(code.trim())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("TOTP verification error", e);
        }
        return false;
    }

    private String generateTotp(byte[] key, long interval)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] data = ByteBuffer.allocate(8).putLong(interval).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % (int) Math.pow(10, TOTP_DIGITS);
        return String.format("%0" + TOTP_DIGITS + "d", otp);
    }

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private String base64ToBase32(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_CHARS.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }
}
