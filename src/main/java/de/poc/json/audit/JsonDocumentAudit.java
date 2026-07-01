package de.poc.json.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "json_documents_audit")
class JsonDocumentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_old", columnDefinition = "jsonb", nullable = true)
    private Map<String, Object> dataOld;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_new", columnDefinition = "jsonb", nullable = true)
    private Map<String, Object> dataNew;

    @CreationTimestamp
    @Column(name = "audited_at", nullable = false, updatable = false)
    private Instant auditedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 32, nullable = false)
    private ActionType actionType;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    protected JsonDocumentAudit() {
        // für Hibernate
    }

    public JsonDocumentAudit(UUID documentId,
                             Map<String, Object> dataOld, Map<String, Object> dataNew,
                             ActionType actionType, UUID userId) {
        this.documentId = documentId;
        this.dataOld = dataOld;
        this.dataNew = dataNew;
        this.actionType = actionType;
        this.userId = userId;
    }
}