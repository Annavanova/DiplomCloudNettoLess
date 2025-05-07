package DiplomCloud.DiplomCloud.services;

import DiplomCloud.DiplomCloud.dto.AuthRequest;
import DiplomCloud.DiplomCloud.models.User;
import DiplomCloud.DiplomCloud.repositories.UserRepository;
import DiplomCloud.DiplomCloud.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public User createUser(String username, String password) {
        log.info("Создание нового пользователя: {}", username);
        User user = new User();
        user.setLogin(username);
        user.setPassword(passwordEncoder.encode(password));
        User savedUser = userRepository.save(user);
        log.debug("Пользователь успешно создан с id: {}", savedUser.getId());
        return savedUser;
    }

    public String authenticate(AuthRequest request) {
        log.info("Попытка аутентификации пользователя: {}", request.getLogin());
        User user = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> {
                    log.warn("Ошибка аутентификации - пользователь не найден: {}", request.getLogin());
                    return new BadCredentialsException("Не правильный логин или пароль");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Ошибка аутентификации - неверный пароль для пользователя: {}", request.getLogin());
            throw new BadCredentialsException("Не правильный логин или пароль");
        }

        String token = jwtTokenProvider.generateToken(request.getLogin());
        log.debug("Проверка подлинности прошла успешно для использования: {}", request.getLogin());
        return token;
    }

    public void logout(String token) {
        log.info("Запрос на получение токена для выхода из системы");
        jwtTokenProvider.invalidateToken(token);
        log.debug("Токен признан недействительным");
    }
}
