package com.assessment.spruceid.verifier.service.impl;

import com.assessment.spruceid.verifier.model.Nonce;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NonceServiceImplTest {

    @Test
    void issue() {
        NonceServiceImpl service = new NonceServiceImpl(300);
        Nonce nonce = service.issue();
        assertNotNull(nonce);
    }

    @Test
    void validateAndConsume() {
        NonceServiceImpl service = new NonceServiceImpl(300);
        Nonce nonce = service.issue();
        assertTrue(service.validateAndConsume(nonce.getNonce()));
        assertFalse(service.validateAndConsume(nonce.getNonce()));
    }
}