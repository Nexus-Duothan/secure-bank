package com.securebank.totp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

@Component
public class TotpEngine {

  private static final int TIME_STEP_SECONDS = 30;
  private static final int CODE_DIGITS = 6;
  private static final String HMAC_ALGORITHM = "HmacSHA1";
  private static final String SCRATCH_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  private final Base32 base32 = new Base32();
  private final SecureRandom secureRandom = new SecureRandom();

  public String generateSecretKey() {
    byte[] bytes = new byte[20]; // 160 bits
    secureRandom.nextBytes(bytes);
    return base32.encodeToString(bytes).replace("=", "");
  }

  public List<String> generateScratchCodes(int count) {
    List<String> codes = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      StringBuilder sb = new StringBuilder(8);
      for (int j = 0; j < 8; j++) {
        sb.append(SCRATCH_CODE_CHARS.charAt(secureRandom.nextInt(SCRATCH_CODE_CHARS.length())));
      }
      codes.add(sb.toString());
    }
    return codes;
  }

  public String generateOtpauthUrl(String secretKey, String accountName, String issuer) {
    return String.format(
      "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
      issuer,
      accountName,
      secretKey,
      issuer
    );
  }

  public byte[] generateQrCodePng(String otpauthUrl, int width, int height) {
    try {
      QRCodeWriter qrCodeWriter = new QRCodeWriter();
      BitMatrix bitMatrix = qrCodeWriter.encode(otpauthUrl, BarcodeFormat.QR_CODE, width, height);
      ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
      return pngOutputStream.toByteArray();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to generate QR code image", e);
    }
  }

  public String generateQrCodeBase64(String otpauthUrl, int width, int height) {
    byte[] imageBytes = generateQrCodePng(otpauthUrl, width, height);
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
  }

  public boolean verifyTotp(String secretKey, String code, int window) {
    if (code == null || code.length() != CODE_DIGITS) {
      return false;
    }

    long currentBucket = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
    byte[] keyBytes = base32.decode(secretKey);

    for (int i = -window; i <= window; i++) {
      long bucket = currentBucket + i;
      String generatedCode = generateTotpForBucket(keyBytes, bucket);
      if (code.equals(generatedCode)) {
        return true;
      }
    }
    return false;
  }

  public String generateTotpForCurrentTime(String secretKey) {
    long currentBucket = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
    byte[] keyBytes = base32.decode(secretKey);
    return generateTotpForBucket(keyBytes, currentBucket);
  }

  private String generateTotpForBucket(byte[] keyBytes, long bucket) {
    try {
      byte[] data = ByteBuffer.allocate(8).putLong(bucket).array();
      SecretKeySpec signKey = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(signKey);
      byte[] hash = mac.doFinal(data);

      int offset = hash[hash.length - 1] & 0xF;
      int binary =
        ((hash[offset] & 0x7F) << 24) |
        ((hash[offset + 1] & 0xFF) << 16) |
        ((hash[offset + 2] & 0xFF) << 8) |
        (hash[offset + 3] & 0xFF);

      int otp = binary % (int) Math.pow(10, CODE_DIGITS);
      return String.format("%0" + CODE_DIGITS + "d", otp);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("Failed to calculate HMAC TOTP", e);
    }
  }
}
