package com.assessment.spruceid.verifier.conf;

import com.assessment.spruceid.verifier.utils.PemUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.interfaces.ECPublicKey;

@Configuration
public class PublicKeyConfig {
    private static final Logger log = LogManager.getLogger(PublicKeyConfig.class);
    private final ResourceLoader resourceLoader;

    public PublicKeyConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Load the verifier's EC public key from a PEM file.
     * The location can be specified via:
     * 1. Environment variable VERIFIER_PUBKEY_PATH
     * 2. Spring property verifier.pubkey.path
     * 3. Defaults to classpath:pub.pem
     * <p>
     * Supports classpath:, file:, http:, and https: URLs.
     * For http(s), connection and read timeouts can be set via
     * verifier.pubkey.connect.timeout and verifier.pubkey.read.timeout (in ms).
     *
     * @param location       the location of the PEM file
     * @param connectTimeout connection timeout
     * @param readTimeout    read timeout
     * @return the loaded ECPublicKey
     */
    @Bean
    public ECPublicKey verifierPublicKey(@Value("${VERIFIER_PUBKEY_PATH:${verifier.pubkey.path:classpath:pub.pem}}") String location,
                                         @Value("${verifier.pubkey.connect.timeout:5000}") int connectTimeout,
                                         @Value("${verifier.pubkey.read.timeout:5000}") int readTimeout) {
        Resource r = resolve(location);
        try (InputStream in = open(r, location, connectTimeout, readTimeout)) {
            return PemUtil.readEcPublicKeyFromPem(in);
        } catch (Exception e) {
            log.error("Failed to load EC public key from {}", location, e);
            throw new IllegalStateException("Cannot load EC public key from: " + location, e);
        }
    }

    /**
     * Resolve a location to a resource.
     *
     * @param location the location to resolve
     * @return the resolved resource
     */
    private Resource resolve(String location) {
        if (location == null || location.isBlank())
            return new ClassPathResource("pub.pem");

        // if no scheme prefix, treat as a filesystem path (supports ~/…)
        if (!location.matches("^[a-zA-Z]+:.*")) {
            String path = location.startsWith("~/")
                    ? System.getProperty("user.home") + location.substring(1)
                    : location;
            return new FileSystemResource(path);
        }
        return resourceLoader.getResource(location);
    }

    /**
     * Open an input stream for the given resource.
     *
     * @param res            the resource to open
     * @param location       the location of the resource
     * @param connectTimeout connection timeout
     * @param readTimeout    read timeout
     * @return the opened input stream
     * @throws IOException if opening the stream fails
     */
    private InputStream open(Resource res, String location, int connectTimeout, int readTimeout) throws IOException {
        if (res instanceof UrlResource && (location.startsWith("http://") || location.startsWith("https://"))) {
            URLConnection c = ((UrlResource) res).getURL().openConnection();
            c.setConnectTimeout(connectTimeout);
            c.setReadTimeout(readTimeout);
            return c.getInputStream();
        }
        if (!res.exists()) throw new FileNotFoundException("Resource not found: " + safeDescribe(res));

        return res.getInputStream();
    }

    /**
     * Safe describes a resource.
     *
     * @param r the resource
     * @return a safe description of the resource
     */
    private String safeDescribe(Resource r) {
        try {
            return r.getDescription();
        } catch (Exception ignored) {
            return r.toString();
        }
    }
}
