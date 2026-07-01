package de.poc.json.audit;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
class DefaultAuditService implements AuditService {

    // Mock-User, bis echte Authentifizierung vorhanden ist
    private static final UUID MOCK_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final JsonDocumentAuditRepository repository;

    DefaultAuditService(JsonDocumentAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public void recordInsert(UUID documentId, Map<String, Object> newData) {
        repository.save(new JsonDocumentAudit(documentId, null, newData, ActionType.INSERT, MOCK_USER_ID));
    }

    @Override
    public void recordUpdate(UUID documentId, Map<String, Object> oldData, Map<String, Object> newData) {
        repository.save(new JsonDocumentAudit(documentId, oldData, newData, ActionType.UPDATE, MOCK_USER_ID));
    }

    @Override
    public void recordDelete(UUID documentId, Map<String, Object> oldData) {
        repository.save(new JsonDocumentAudit(documentId, oldData, null, ActionType.DELETE, MOCK_USER_ID));
    }
}
