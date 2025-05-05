package DiplomCloud.DiplomCloud.security;


import DiplomCloud.DiplomCloud.models.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long validityInMilliseconds;

    private final Set<String> invalidatedTokens = new HashSet<>(); // Хранилище недействительных токенов

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long validityInMilliseconds) {

        // Генерация безопасного ключа из секретной строки
        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(
                        Base64.getEncoder().encodeToString(secret.getBytes())
                )
        );
        this.validityInMilliseconds = validityInMilliseconds;

        // Проверка длины ключа
        if (secretKey.getEncoded().length < 32) { // 256 бит = 32 байта
            throw new IllegalArgumentException("Секретный ключ должен быть не менее 256 бит (32 символа)");
        }
    }
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + validityInMilliseconds))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    /**
     * Добавляет токен в черный список
     */
    public void invalidateToken(String token) {
        invalidatedTokens.add(token);
    }

    /**
     * Проверяет, не был ли токен отозван
     */
    public boolean isTokenValid(String token) {
        return !invalidatedTokens.contains(token);
    }

    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
