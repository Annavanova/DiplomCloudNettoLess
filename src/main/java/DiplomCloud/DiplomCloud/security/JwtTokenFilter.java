package DiplomCloud.DiplomCloud.security;

import DiplomCloud.DiplomCloud.exception.InvalidJwtAuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        try {
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        jwtTokenProvider.getUsername(token),
                        null,
                        Collections.emptyList()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (InvalidJwtAuthenticationException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Неправильный JWT token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("auth-token");
        if (bearerToken != null) {
            return bearerToken;
        }
        return null;
    }
}
