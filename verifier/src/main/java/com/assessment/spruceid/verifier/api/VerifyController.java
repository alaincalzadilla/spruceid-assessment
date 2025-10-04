package com.assessment.spruceid.verifier.api;

import com.assessment.spruceid.verifier.dto.VerifyRequest;
import com.assessment.spruceid.verifier.dto.VerifyResponse;
import com.assessment.spruceid.verifier.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VerifyController {
    private final VerifyService verifyService;

    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<VerifyResponse>> verify(@RequestBody VerifyRequest req) {
        return verifyService.verify(req);
    }
}
