package com.assessment.spruceid.verifier.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Nonce {
    private String nonce;
    private Instant issuedAt;
    private Instant expiresAt;
    volatile boolean used;
}
