package DiplomCloud.DiplomCloud.services;

import DiplomCloud.DiplomCloud.dto.AuthRequest;
import DiplomCloud.DiplomCloud.dto.AuthResponse;
import DiplomCloud.DiplomCloud.models.User;
import DiplomCloud.DiplomCloud.repositories.UserRepository;
import DiplomCloud.DiplomCloud.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public User createUser(String username, String password) {
        User user = new User();
        user.setLogin(username);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new BadCredentialsException("Не правильный логин или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Не правильный логин или пароль");
        }

        String token = jwtTokenProvider.generateToken(request.getLogin());
        return new AuthResponse(token);
    }

    public void logout(String token) {
        jwtTokenProvider.invalidateToken(token);
    }
}
