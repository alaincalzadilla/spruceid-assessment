package com.assessment.spruceid.verifier.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class PemUtil {
    private PemUtil() {
    }

    /**
     * Read an EC public key from a PEM-formatted input stream.
     *
     * @param in the input stream containing the PEM data
     * @return the ECPublicKey
     * @throws Exception if an error occurs during reading or parsing
     */
    public static ECPublicKey readEcPublicKeyFromPem(InputStream in) throws Exception {
        String pem = new String(in.readAllBytes(), StandardCharsets.US_ASCII);
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        var kf = KeyFactory.getInstance("EC");
        PublicKey pk = kf.generatePublic(new X509EncodedKeySpec(der));
        return (ECPublicKey) pk;
    }
}
