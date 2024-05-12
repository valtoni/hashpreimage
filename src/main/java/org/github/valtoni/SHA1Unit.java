package org.github.valtoni;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

public class SHA1Unit {

    private final byte[] input;
    private final byte[] digest;

    public SHA1Unit() {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.input = sha1.digest(new byte[64]);
        Random random = new Random();
        random.nextBytes(input);
        this.digest = sha1.digest(input);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String hexDigest() {
        return bytesToHex(digest);
    }

    public String hexInput() {
        return bytesToHex(input);
    }

    public boolean equals(SHA1Unit other) {
        return Arrays.equals(digest, other.digest);
    }

}