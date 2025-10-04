package com.assessment.spruceid.verifier.service;

import com.assessment.spruceid.verifier.dto.VerifyRequest;
import com.assessment.spruceid.verifier.dto.VerifyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

public interface VerifyService {
    Mono<ResponseEntity<VerifyResponse>> verify(@RequestBody VerifyRequest req);
}
