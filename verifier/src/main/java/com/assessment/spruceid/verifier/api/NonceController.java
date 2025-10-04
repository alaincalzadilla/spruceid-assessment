package com.assessment.spruceid.verifier.api;

import com.assessment.spruceid.verifier.dto.NonceResponse;
import com.assessment.spruceid.verifier.model.Nonce;
import com.assessment.spruceid.verifier.service.NonceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/nonce")
@RequiredArgsConstructor
public class NonceController {
    private final NonceService nonceService;

    /**
     * Get nonce. Issue one-time nonce (global, single trusted key).
     *
     * @return Mono of NonceResponse
     */
    @GetMapping(value = "/issue", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<NonceResponse> getNonce() {
        Nonce n = nonceService.issue();
        return Mono.just(new NonceResponse(n.getNonce(), n.getIssuedAt().toEpochMilli(), n.getExpiresAt().toEpochMilli()));
    }
}
