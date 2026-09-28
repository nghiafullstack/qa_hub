package vn.qahub.api.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Sinh/kiểm tra token ingestion cho từng dự án. Token gốc (dạng {@code qah_<random>}) CHỈ trả về
 * 1 LẦN lúc tạo — DB chỉ lưu hash SHA-256, giống cách token thường được quản lý (GitHub PAT,
 * Stripe API key...), để lộ DB không đồng nghĩa lộ token dùng được ngay.
 */
@Service
@RequiredArgsConstructor
public class ApiTokenService {

    private static final String TOKEN_PREFIX = "qah_";
    private final SecureRandom secureRandom = new SecureRandom();

    private final ApiTokenRepository apiTokenRepository;

    public record IssuedToken(ApiToken entity, String rawToken) {
    }

    @Transactional
    public IssuedToken issueToken(Long projectId, String name) {
        String rawToken = TOKEN_PREFIX + randomUrlSafe(32);
        ApiToken token = new ApiToken();
        token.setProjectId(projectId);
        token.setName(name);
        token.setTokenHash(hash(rawToken));
        apiTokenRepository.save(token);
        return new IssuedToken(token, rawToken);
    }

    /** Trả về {@code projectId} nếu token hợp lệ, cập nhật {@code lastUsedAt}. */
    @Transactional
    public Optional<Long> resolveProjectId(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return apiTokenRepository.findByTokenHash(hash(rawToken))
                .map(token -> {
                    token.setLastUsedAt(Instant.now());
                    apiTokenRepository.save(token);
                    return token.getProjectId();
                });
    }

    private String randomUrlSafe(int numBytes) {
        byte[] bytes = new byte[numBytes];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }
}
