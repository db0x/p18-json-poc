package de.poc.json.audit;

import java.util.Map;
import java.util.UUID;

/**
 * Public API of the audit domain. Other domains record document changes through
 * this interface and stay unaware of how auditing is persisted or which user is
 * attributed to the change.
 */
public interface AuditService {

    void recordInsert(UUID documentId, Map<String, Object> newData);

    void recordUpdate(UUID documentId, Map<String, Object> oldData, Map<String, Object> newData);

    void recordDelete(UUID documentId, Map<String, Object> oldData);
}