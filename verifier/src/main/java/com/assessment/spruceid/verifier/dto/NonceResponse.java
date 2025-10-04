package com.assessment.spruceid.verifier.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NonceResponse {
    private String nonce; // challenge proves liveness and prevents replay
    private long issuedAt; // created time
    private long expiresAt; // expiration time
}
