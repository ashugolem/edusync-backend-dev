package com.project.edusync.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Component
public class JWTFilter extends OncePerRequestFilter {

    // 1. We ONLY need AuthUtil. We do not need UserRepository.
    private final AuthUtil authUtil;
    private final CustomUserDetailService customUserDetailService;

    // We also do not need ObjectMapper. Error handling is
    // delegated to the JwtAuthenticationEntryPoint.

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        if (requestTokenHeader == null || !requestTokenHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = requestTokenHeader.substring(7); // "Bearer "
        String username = null;
        List<GrantedAuthority> authorities = null;

        try {
            // 2. Extract username AND authorities *directly from the token*
            username = authUtil.getUsernameFromToken(token);
            authorities = authUtil.getAuthoritiesFromToken(token); // NO DATABASE CALL

        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            // Let the JwtAuthenticationEntryPoint handle the 401 response
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return;
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        } catch (Exception e) {
            log.error("An unexpected error occurred during JWT processing: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing token");
            return;
        }

        // 3. Check if username & authorities were found and context is not set
        if (username != null && authorities != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = customUserDetailService.loadUserByUsername(username);

            // 4. Create the authentication token
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,      // Credentials (N/A for JWT)
                    authorities // Authorities (from the token)
            );

            // 5. Set the Authentication in SecurityContextHolder
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // 6. Move to the next filter
        filterChain.doFilter(request, response);
    }
}