package de.poc.json.audit;

import org.springframework.data.repository.Repository;

import java.util.UUID;

/**
 * Insert-only-Repository für Audit-Einträge. Bewusst kein {@link org.springframework.data.jpa.repository.JpaRepository},
 * damit hier weder Update-, Delete- noch Read-Methoden zur Verfügung stehen – die Audit-Daten werden
 * ausschließlich geschrieben und später von einem anderen Prozess gelesen.
 */
interface JsonDocumentAuditRepository extends Repository<JsonDocumentAudit, UUID> {

    JsonDocumentAudit save(JsonDocumentAudit audit);
}