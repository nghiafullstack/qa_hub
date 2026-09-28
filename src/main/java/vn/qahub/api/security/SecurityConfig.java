package vn.qahub.api.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import vn.qahub.api.token.ApiTokenService;

/**
 * 2 filter chain TÁCH RIÊNG vì có 2 loại "người gọi" hoàn toàn khác nhau:
 * <ul>
 *   <li>{@code /api/v1/ingest/**} — CI của 1 dự án gửi kết quả test, xác thực bằng token riêng
 *       dự án ({@link ProjectTokenAuthenticationFilter}), KHÔNG phải người dùng đăng nhập.</li>
 *   <li>Còn lại — dashboard, xác thực bằng JWT phiên đăng nhập ({@link JwtAuthenticationFilter}).</li>
 * </ul>
 * Order thấp hơn được thử trước — {@code ingestChain} (order 1) chỉ áp dụng đúng path của nó nhờ
 * {@code securityMatcher}, request khác rơi qua {@code dashboardChain} (order 2).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final ApiTokenService apiTokenService;

    /** Origin của dashboard Next.js — dev mặc định :3000, đổi khi deploy thật (không dùng "*" vì có gửi JWT). */
    @Value("${qahub.web-origin:http://localhost:3000}")
    private String webOrigin;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Chỉ áp dụng cho {@code dashboardChain} — {@code ingestChain} là server-to-server (CI), không cần CORS. */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(webOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain ingestChain(HttpSecurity http) throws Exception {
        http.securityMatcher(new AntPathRequestMatcher("/api/v1/ingest/**"))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("PROJECT_INGEST"))
                .addFilterBefore(
                        new ProjectTokenAuthenticationFilter(apiTokenService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain dashboardChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
