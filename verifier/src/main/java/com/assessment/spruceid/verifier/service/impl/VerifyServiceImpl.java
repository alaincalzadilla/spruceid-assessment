package com.assessment.spruceid.verifier.service.impl;

import com.assessment.spruceid.verifier.dto.VerifyRequest;
import com.assessment.spruceid.verifier.dto.VerifyResponse;
import com.assessment.spruceid.verifier.service.NonceService;
import com.assessment.spruceid.verifier.service.VerifyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

@Service
public class VerifyServiceImpl implements VerifyService {
    private static final Logger log = LogManager.getLogger(VerifyServiceImpl.class);
    private final NonceService nonceService;
    private final ECPublicKey verifierPublicKey;

    public VerifyServiceImpl(NonceService nonceService, ECPublicKey verifierPublicKey) {
        this.nonceService = nonceService;
        this.verifierPublicKey = verifierPublicKey;
    }

    /**
     * Verify the signature over the nonce using the trusted public key.
     * Body: { "nonce": "...", "sigBase64Url": "..." }
     * Steps:
     * 1. Check nonce freshness and consume it (prevents replay).
     * 2. Verify ECDSA signature over EXACT ASCII: nonce, using pre-installed public key.
     *
     * @param req VerifyRequest
     * @return Mono of ResponseEntity<VerifyResponse>
     */
    @Override
    public Mono<ResponseEntity<VerifyResponse>> verify(VerifyRequest req) {
        try {
            if (req == null || req.getNonce() == null || req.getSigBase64Url() == null)
                throw new IllegalArgumentException("Invalid request: missing required fields.");

            log.info("Received request: {}", req);
            // Validate & consume nonce first (replay protection)
            if (!nonceService.validateAndConsume(req.getNonce())) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(VerifyResponse.builder()
                                .verified(false)
                                .message("The nonce is invalid or has already been consumed.")
                                .build()));
            }

            // Verify ECDSA signature over ASCII "nonce" with ES256
            byte[] msg = req.getNonce().getBytes(StandardCharsets.US_ASCII);
            byte[] sigDer = decodeB64Flexible(req.getSigBase64Url());

            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(verifierPublicKey);
            s.update(msg);
            boolean ok = s.verify(sigDer);
            if (!ok) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(VerifyResponse.builder()
                                .verified(false)
                                .message("Signature verification failed.")
                                .build()));
            }

            return Mono.just(ResponseEntity.ok(new VerifyResponse(true, null)));

        } catch (Exception e) {
            log.error("Request processing failed.", e);
            return Mono.just(ResponseEntity.badRequest()
                    .body(VerifyResponse.builder()
                            .verified(false)
                            .message("Request processing failed. Invalid request: " + e.getMessage())
                            .build()));
        }
    }

    private static byte[] decodeB64Flexible(String s) {
        try {
            return Base64.getUrlDecoder().decode(s);   // works if holder sends base64url
        } catch (IllegalArgumentException ignore) {
            return Base64.getDecoder().decode(s);      // fallback for standard base64 (+,/)
        }
    }
}
