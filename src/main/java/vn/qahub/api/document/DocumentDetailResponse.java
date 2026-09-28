package vn.qahub.api.document;

import java.time.Instant;

public record DocumentDetailResponse(
        Long id, String sourceType, String sourceRef, String title, String docFormat,
        String contentText, Instant lastSyncedAt) {

    public static DocumentDetailResponse from(Document document) {
        return new DocumentDetailResponse(
                document.getId(), document.getSourceType().name(), document.getSourceRef(),
                document.getTitle(), document.getDocFormat(), document.getContentText(),
                document.getLastSyncedAt());
    }
}
