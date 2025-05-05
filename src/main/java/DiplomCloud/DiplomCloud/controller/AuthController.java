package DiplomCloud.DiplomCloud.controller;

import DiplomCloud.DiplomCloud.dto.AuthRequest;
import DiplomCloud.DiplomCloud.dto.ErrorResponse;
import DiplomCloud.DiplomCloud.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cloud")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthRequest request) {
        try {
            return ResponseEntity.ok(authService.authenticate(request));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage(), 400));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
}
