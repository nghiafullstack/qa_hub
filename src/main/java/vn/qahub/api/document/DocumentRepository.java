package vn.qahub.api.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByProjectId(Long projectId);

    List<Document> findByProjectIdAndSourceRef(Long projectId, String sourceRef);
}
