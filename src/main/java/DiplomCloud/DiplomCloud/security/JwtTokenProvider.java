package DiplomCloud.DiplomCloud.security;


import DiplomCloud.DiplomCloud.exception.InvalidJwtAuthenticationException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;

@Component
@Slf4j
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long validityInMilliseconds;
    private final Set<String> invalidatedTokens = Collections.synchronizedSet(new HashSet<>()); // Хранилище недействительных токенов

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long validityInMilliseconds) {

        // Генерация безопасного ключа из секретной строки
        log.info("Инициализация JwtTokenProvider");
        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(
                        Base64.getEncoder().encodeToString(secret.getBytes())
                )
        );
        this.validityInMilliseconds = validityInMilliseconds;

        // Проверка длины ключа
        if (secretKey.getEncoded().length < 32) { // 256 бит = 32 байта
            log.error("Секретный ключ должен быть не менее 256 бит (32 символа)");
            throw new IllegalArgumentException("Секретный ключ должен быть не менее 256 бит (32 символа)");
        }
    }
    public String generateToken(String username) {
        log.debug("Генерирование токена для использованияr: {}", username);
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return !invalidatedTokens.contains(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Недопустимый  JWT токен: {}", e.getMessage());
            return false;
        }
    }
    //Добавляет токен в черный список
    public void invalidateToken(String token) {
        log.debug("Недопустимый токен");
        invalidatedTokens.add(token);
    }

    public String getUsername(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException e) {
            log.error("Не удалось получить имя пользователя из токена: {}", e.getMessage());
            throw new InvalidJwtAuthenticationException("Недопустимый токен");
        }
    }
}
