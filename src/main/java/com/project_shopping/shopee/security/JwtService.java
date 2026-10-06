package com.project_shopping.shopee.security;

import com.project_shopping.shopee.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final System.Logger LOGGER =
            System.getLogger(JwtService.class.getName());

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {

        String signingSecret = resolveSigningSecret(secret);
        byte[] secretBytes = signingSecret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                    "app.jwt.secret phải có ít nhất 32 byte."
            );
        }

        if (expirationMs <= 0) {
            throw new IllegalArgumentException(
                    "app.jwt.expiration-ms phải lớn hơn 0."
            );
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = expirationMs;
    }

    private String resolveSigningSecret(String configuredSecret) {

        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return configuredSecret;
        }

        byte[] generatedSecret = new byte[32];
        SECURE_RANDOM.nextBytes(generatedSecret);

        LOGGER.log(
                System.Logger.Level.WARNING,
                "JWT_SECRET chưa được cấu hình. Ứng dụng đang dùng khóa tạm; "
                        + "token sẽ hết hiệu lực sau khi khởi động lại. Hãy đặt "
                        + "JWT_SECRET thành giá trị ngẫu nhiên ổn định khi triển khai."
        );

        return Base64.getEncoder().encodeToString(generatedSecret);
    }

    public String createToken(User user) {

        Instant now = Instant.now();
        Instant expirationTime = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(key)
                .compact();
    }

    public String extractSubject(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
