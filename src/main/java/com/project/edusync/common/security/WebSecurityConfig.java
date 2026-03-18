package com.project.edusync.common.security;

import com.project.edusync.common.exception.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    @Value("${api.url}")
    private String apiVersionPath;

    // Grouping whitelisted URLs for clarity
    private static final String[] AUTH_WHITELIST = {
            "/auth/**",
            "/public/**" // For any future public-facing school data
    };

    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private static final String[] MONITORING_WHITELIST = {
            "/actuator/**" // Essential for health checks and metrics
    };

    // SSE stream for bulk import progress.
    // EventSource (browser API) cannot send Authorization headers,
    // so the stream path is open. The sessionId UUID is the access token.
    private static final String[] SSE_WHITELIST = {
            "/bulk-import/stream/**"
    };

    private final JWTFilter jwtFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Recommendation: Move origins to application.yml for environment-specific configs
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomAuthenticationEntryPoint customAuthenticationEntryPoint) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 1. Public Auth Endpoints
                        .requestMatchers(Arrays.stream(AUTH_WHITELIST)
                                .map(path -> apiVersionPath + path)
                                .toArray(String[]::new)).permitAll()

                        // 2. API Documentation (Usually not prefixed by apiVersionPath)
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()

                        // 3. Actuator Endpoints (Monitoring)
                        .requestMatchers(MONITORING_WHITELIST).permitAll()

                        // 4. SSE stream for bulk import progress (EventSource cannot send JWT headers)
                        .requestMatchers(Arrays.stream(SSE_WHITELIST)
                                .map(path -> apiVersionPath + path)
                                .toArray(String[]::new)).permitAll()

                        // 5. Default: All other requests must be authenticated
//                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
}