package com.assessment.spruceid.verifier.service;

import com.assessment.spruceid.verifier.model.Nonce;

public interface NonceService {
    Nonce issue();

    boolean validateAndConsume(String nonce);
}
