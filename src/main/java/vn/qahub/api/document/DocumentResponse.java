package vn.qahub.api.document;

import java.time.Instant;

public record DocumentResponse(
        Long id, String sourceType, String sourceRef, String title, String docFormat,
        Integer contentLength, Instant lastSyncedAt) {

    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(), document.getSourceType().name(), document.getSourceRef(),
                document.getTitle(), document.getDocFormat(),
                document.getContentText() == null ? 0 : document.getContentText().length(),
                document.getLastSyncedAt());
    }
}
