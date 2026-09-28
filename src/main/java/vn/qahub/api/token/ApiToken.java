package vn.qahub.api.token;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token 1 dự án dùng để tự xác thực khi gửi kết quả test vào ingestion API. Chỉ lưu
 * {@code tokenHash} (SHA-256) — token gốc chỉ hiện 1 lần lúc tạo, không lưu lại được.
 */
@Entity
@Table(name = "api_tokens")
@Getter
@Setter
@NoArgsConstructor
public class ApiToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    private String name;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
