package com.assessment.spruceid.verifier.service.impl;

import com.assessment.spruceid.verifier.model.Nonce;
import com.assessment.spruceid.verifier.service.NonceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NonceServiceImpl implements NonceService {
    private final SecureRandom rnd = new SecureRandom();
    private final Duration ttl;
    private final AtomicReference<Nonce> current = new AtomicReference<>();

    public NonceServiceImpl(@Value("${nonce.ttl.seconds:300}") long ttlSeconds) {
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Nonce issue() {
        byte[] b = new byte[32]; rnd.nextBytes(b);
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        Instant now = Instant.now();
        Nonce nonce = new Nonce(n, now, now.plus(ttl),false);
        current.set(nonce);
        return nonce;
    }

    public boolean validateAndConsume(String nonce) {
        Nonce n = current.get();
        if (n == null) return false;
        if (n.isUsed()) return false;
        if (Instant.now().isAfter(n.getExpiresAt())) { current.set(null); return false; }
        if (!n.getNonce().equals(nonce)) return false;
        n.setUsed(true);
        current.set(null);
        return true;
    }
}
