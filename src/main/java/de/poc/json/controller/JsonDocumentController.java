package de.poc.json.controller;

import de.poc.json.entity.JsonDocument;
import de.poc.json.repository.JsonDocumentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class JsonDocumentController {

    private final JsonDocumentRepository repository;

    public JsonDocumentController(JsonDocumentRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<UUID> push(@RequestBody Map<String, Object> data) {
        UUID id = UUID.randomUUID();
        data.put("uuid", id.toString());

        JsonDocument document = new JsonDocument();
        document.setId(id);
        document.setData(data);
        repository.save(document);

        return ResponseEntity.ok(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(document -> ResponseEntity.ok(document.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID id, @RequestBody Map<String, Object> data) {
        return repository.findById(id)
                .map(document -> {
                    data.put("uuid", id.toString());
                    document.setData(data);
                    repository.save(document);
                    return ResponseEntity.ok(data);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<JsonDocument>> searchByFieldValue(@RequestParam String field, @RequestParam String value) {
        if ("name".equals(field)) {
            return ResponseEntity.ok(repository.findByName(value));
        }
        return ResponseEntity.ok(repository.findByFieldValue(field, value));
    }
}
