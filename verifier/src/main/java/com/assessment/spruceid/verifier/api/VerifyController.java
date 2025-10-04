package com.assessment.spruceid.verifier.api;

import com.assessment.spruceid.verifier.dto.VerifyRequest;
import com.assessment.spruceid.verifier.dto.VerifyResponse;
import com.assessment.spruceid.verifier.service.VerifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VerifyController {
    private final VerifyService verifyService;

    @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Mono<ResponseEntity<VerifyResponse>> verify(@RequestBody VerifyRequest req) {
        return verifyService.verify(req);
    }
}
