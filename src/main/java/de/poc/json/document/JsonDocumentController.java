package de.poc.json.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "JSON Documents", description = "Store, retrieve, update and search JSON documents")
public class JsonDocumentController {

    private static final Logger log = LoggerFactory.getLogger(JsonDocumentController.class);

    private final JsonDocumentService service;

    public JsonDocumentController(JsonDocumentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create document",
            description = "Stores a new JSON document and returns its generated UUID.")
    @ApiResponse(responseCode = "200", description = "Document created, UUID in body")
    public ResponseEntity<UUID> push(@RequestBody Map<String, Object> data) {
        UUID id = service.push(data);
        log.info("Created document {}", id);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document", description = "Returns the JSON document for the given UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document found"),
            @ApiResponse(responseCode = "404", description = "No document with this UUID")
    })
    public ResponseEntity<Map<String, Object>> get(
            @Parameter(description = "UUID of the document") @PathVariable UUID id) {
        log.info("Get document {}", id);
        return service.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Document {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update document",
            description = "Replaces the content of the document with the given UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document updated"),
            @ApiResponse(responseCode = "404", description = "No document with this UUID")
    })
    public ResponseEntity<Map<String, Object>> update(
            @Parameter(description = "UUID of the document") @PathVariable UUID id,
            @RequestBody Map<String, Object> data) {
        log.info("Update document {}", id);
        return service.update(id, data)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Document {} not found for update", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document",
            description = "Deletes the document with the given UUID and records a DELETE audit entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Document deleted"),
            @ApiResponse(responseCode = "404", description = "No document with this UUID")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the document") @PathVariable UUID id) {
        if (service.delete(id)) {
            log.info("Deleted document {}", id);
            return ResponseEntity.noContent().build();
        }
        log.warn("Document {} not found for delete", id);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search documents",
            description = "Searches for documents where the given field has the given value.")
    @ApiResponse(responseCode = "200", description = "List of matches (may be empty)")
    public ResponseEntity<List<JsonDocument>> searchByFieldValue(
            @Parameter(description = "Name of the JSON field, e.g. \"name\" or \"city\"") @RequestParam String field,
            @Parameter(description = "Value to match the field against") @RequestParam String value) {
        List<JsonDocument> result = service.search(field, value);
        log.info("Search field='{}' value='{}' returned {} hits", field, value, result.size());
        return ResponseEntity.ok(result);
    }
}
