package de.poc.json.document;

import de.poc.json.audit.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class JsonDocumentService {

    private final JsonDocumentRepository repository;
    private final AuditService auditService;

    public JsonDocumentService(JsonDocumentRepository repository,
                               AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public UUID push(Map<String, Object> data) {
        UUID id = UUID.randomUUID();
        data.put("uuid", id.toString());

        JsonDocument document = new JsonDocument();
        document.setId(id);
        document.setData(data);
        repository.save(document);

        auditService.recordInsert(id, data);

        return id;
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> get(UUID id) {
        return repository.findById(id).map(JsonDocument::getData);
    }

    @Transactional
    public Optional<Map<String, Object>> update(UUID id, Map<String, Object> data) {
        return repository.findById(id).map(document -> {
            Map<String, Object> oldData = document.getData();
            data.put("uuid", id.toString());

            // no actual change -> neither persist nor write an audit entry
            if (data.equals(oldData)) {
                return oldData;
            }

            document.setData(data);
            repository.save(document);

            auditService.recordUpdate(id, oldData, data);

            return data;
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        return repository.findById(id).map(document -> {
            Map<String, Object> oldData = document.getData();
            repository.delete(document);

            auditService.recordDelete(id, oldData);

            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public Page<JsonDocument> list(int page, int size, String query) {
        // stable ordering by id so paging stays consistent across requests
        if (query == null || query.isBlank()) {
            return repository.findAll(PageRequest.of(page, size, Sort.by("id")));
        }
        // full-text-like filter over the JSON values; ordering is baked into the query
        return repository.searchValues(query.trim(), PageRequest.of(page, size));
    }

    /**
     * Zero-based index of the page that contains the given document, using the same
     * id ordering as {@link #list(int, int)}. Empty if no such document exists.
     */
    @Transactional(readOnly = true)
    public Optional<Integer> pageOf(UUID id, int size) {
        if (!repository.existsById(id)) {
            return Optional.empty();
        }
        long position = repository.countByIdLessThan(id);
        return Optional.of((int) (position / size));
    }

    @Transactional(readOnly = true)
    public List<JsonDocument> search(String field, String value) {
        if ("name".equals(field)) {
            return repository.findByName(value);
        }
        return repository.findByFieldValue(field, value);
    }
}
