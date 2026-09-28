package vn.qahub.api.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import vn.qahub.api.token.ApiTokenService;

/**
 * Xác thực request gửi kết quả test (ingestion API) qua token riêng từng dự án, header
 * {@code Authorization: Bearer qah_...} — KHÁC JWT phiên đăng nhập dashboard
 * ({@link JwtAuthenticationFilter}). Gắn {@code projectId} đã xác thực vào request attribute
 * {@value #PROJECT_ID_ATTRIBUTE} để controller đọc lại, tránh tin theo {@code projectId} trong
 * path nếu sau này path/token lệch nhau.
 */
@RequiredArgsConstructor
public class ProjectTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String PROJECT_ID_ATTRIBUTE = "qahub.authenticatedProjectId";

    private final ApiTokenService apiTokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String rawToken = header.substring("Bearer ".length());
            Optional<Long> projectId = apiTokenService.resolveProjectId(rawToken);
            if (projectId.isPresent()) {
                request.setAttribute(PROJECT_ID_ATTRIBUTE, projectId.get());
                var authentication = new UsernamePasswordAuthenticationToken(
                        projectId.get(), null, List.of(new SimpleGrantedAuthority("ROLE_PROJECT_INGEST")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
