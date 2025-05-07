package DiplomCloud.DiplomCloud.controller;

import DiplomCloud.DiplomCloud.dto.AuthRequest;
import DiplomCloud.DiplomCloud.dto.AuthResponse;
import DiplomCloud.DiplomCloud.dto.ErrorResponse;
import DiplomCloud.DiplomCloud.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cloud")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthRequest request) {
        log.info("Попытка входа пользователя в систему: {}", request.getLogin());
        try {
            String token = authService.authenticate(request);
            log.info("Вход пользователя в систему завершен успешно: {}", request.getLogin());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (BadCredentialsException e) {
            log.warn("Пользователю не удалось войти в систему: {} - неверные учетные данные", request.getLogin());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        log.info("Получен запрос на выход из системы");
        authService.logout(token);
        log.debug("Выход из системы выполнен успешно");
        return ResponseEntity.ok().build();
    }
}
