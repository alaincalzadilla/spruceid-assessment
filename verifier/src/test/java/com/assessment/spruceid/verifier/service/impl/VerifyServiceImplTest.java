package com.assessment.spruceid.verifier.service.impl;

import com.assessment.spruceid.verifier.dto.VerifyRequest;
import com.assessment.spruceid.verifier.dto.VerifyResponse;
import com.assessment.spruceid.verifier.service.NonceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyServiceImplTest {

    @Mock
    private NonceService nonceService;

    private VerifyServiceImpl verifyService;
    private PrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        // Generate an EC key pair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyGen.generateKeyPair();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        privateKey = keyPair.getPrivate();

        verifyService = new VerifyServiceImpl(nonceService, publicKey);
    }

    @Test
    void verify_withValidSignature_shouldReturnVerifiedTrue() throws Exception {
        // Given
        String nonce = "test-nonce-12345";
        byte[] nonceBytes = nonce.getBytes(StandardCharsets.US_ASCII);

        // Sign the nonce
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(privateKey);
        signer.update(nonceBytes);
        byte[] signature = signer.sign();
        String sigBase64 = Base64.getEncoder().encodeToString(signature);

        VerifyRequest request = VerifyRequest.builder()
                .nonce(nonce)
                .sigBase64Url(sigBase64)
                .build();

        when(nonceService.validateAndConsume(nonce)).thenReturn(true);

        // When
        Mono<ResponseEntity<VerifyResponse>> result = verifyService.verify(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().isVerified()).isTrue();
                    assertThat(response.getBody().getMessage()).isNull();
                })
                .verifyComplete();

        verify(nonceService, times(1)).validateAndConsume(nonce);
    }

    @Test
    void verify_withInvalidNonce_shouldReturnUnauthorized() {
        // Given
        String nonce = "invalid-nonce";
        VerifyRequest request = VerifyRequest.builder()
                .nonce(nonce)
                .sigBase64Url("some-signature")
                .build();

        when(nonceService.validateAndConsume(nonce)).thenReturn(false);

        // When
        Mono<ResponseEntity<VerifyResponse>> result = verifyService.verify(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().isVerified()).isFalse();
                    assertThat(response.getBody().getMessage())
                            .isEqualTo("The nonce is invalid or has already been consumed.");
                })
                .verifyComplete();

        verify(nonceService, times(1)).validateAndConsume(nonce);
    }

    @Test
    void verify_withInvalidSignature_shouldReturnUnauthorized() throws Exception {
        // Given
        String nonce = "test-nonce-12345";

        // Create an invalid signature (sign different data)
        byte[] differentData = "different-data".getBytes(StandardCharsets.US_ASCII);
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(privateKey);
        signer.update(differentData);
        byte[] invalidSignature = signer.sign();
        String sigBase64 = Base64.getEncoder().encodeToString(invalidSignature);

        VerifyRequest request = VerifyRequest.builder()
                .nonce(nonce)
                .sigBase64Url(sigBase64)
                .build();

        when(nonceService.validateAndConsume(nonce)).thenReturn(true);

        // When
        Mono<ResponseEntity<VerifyResponse>> result = verifyService.verify(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().isVerified()).isFalse();
                    assertThat(response.getBody().getMessage())
                            .isEqualTo("Signature verification failed.");
                })
                .verifyComplete();

        verify(nonceService, times(1)).validateAndConsume(nonce);
    }

    @Test
    void verify_withBase64UrlSignature_shouldSucceed() throws Exception {
        // Given
        String nonce = "test-nonce-url-safe";
        byte[] nonceBytes = nonce.getBytes(StandardCharsets.US_ASCII);

        // Sign the nonce
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(privateKey);
        signer.update(nonceBytes);
        byte[] signature = signer.sign();
        String sigBase64Url = Base64.getUrlEncoder().encodeToString(signature);

        VerifyRequest request = VerifyRequest.builder()
                .nonce(nonce)
                .sigBase64Url(sigBase64Url)
                .build();

        when(nonceService.validateAndConsume(nonce)).thenReturn(true);

        // When
        Mono<ResponseEntity<VerifyResponse>> result = verifyService.verify(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().isVerified()).isTrue();
                })
                .verifyComplete();

        verify(nonceService, times(1)).validateAndConsume(nonce);
    }

    @Test
    void verify_withMalformedBase64_shouldReturnBadRequest() {
        // Given
        String nonce = "test-nonce";
        VerifyRequest request = VerifyRequest.builder()
                .nonce(nonce)
                .sigBase64Url("not-valid-base64!@#$%")
                .build();

        when(nonceService.validateAndConsume(nonce)).thenReturn(true);

        // When
        Mono<ResponseEntity<VerifyResponse>> result = verifyService.verify(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().isVerified()).isFalse();
                    assertThat(response.getBody().getMessage())
                            .contains("Request processing failed");
                })
                .verifyComplete();

        verify(nonceService, times(1)).validateAndConsume(nonce);
    }

    @Test
    void verify_withNullNonce_shouldReturnBadRequest() {
        // Given
        VerifyRequest request = VerifyRequest.builder()
                .nonce(null)
                .sigBase64Url("some-signature")
                .build();

        // When
        Mono<ResponseEntity<VerifyResponse>> result = verifyService.verify(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().isVerified()).isFalse();
                    assertThat(response.getBody().getMessage())
                            .contains("Request processing failed");
                })
                .verifyComplete();

        verify(nonceService, never()).validateAndConsume(anyString());
    }

    @Test
    void verify_whenNonceServiceThrowsException_shouldReturnBadRequest() {
        // Given
        String nonce = "test-nonce";
        VerifyRequest request = VerifyRequest.builder()
                .nonce(nonce)
                .sigBase64Url("some-signature")
                .build();

        when(nonceService.validateAndConsume(nonce))
                .thenThrow(new RuntimeException("Service error"));

        // When
        Mono<ResponseEntity<VerifyResponse>> result = verifyService.verify(request);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isNotNull();
                    assertThat(response.getBody().isVerified()).isFalse();
                    assertThat(response.getBody().getMessage())
                            .contains("Request processing failed");
                })
                .verifyComplete();

        verify(nonceService, times(1)).validateAndConsume(nonce);
    }
}