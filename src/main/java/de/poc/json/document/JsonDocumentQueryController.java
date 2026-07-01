package de.poc.json.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-only access to documents. Starts with paginated listing for tabular
 * display and is meant to collect the querying endpoints over time.
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "JSON Documents (read)", description = "Read-only, paginated access to JSON documents")
public class JsonDocumentQueryController {

    static final int MAX_PAGE_SIZE = 10;

    private static final Logger log = LoggerFactory.getLogger(JsonDocumentQueryController.class);

    private final JsonDocumentService service;

    public JsonDocumentQueryController(JsonDocumentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List documents (paginated)",
            description = "Returns one page of documents (at most " + MAX_PAGE_SIZE
                    + " per page) for tabular display, plus metadata to load the next page.")
    @ApiResponse(responseCode = "200", description = "One page of documents")
    public DocumentPage list(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1.." + MAX_PAGE_SIZE + ")") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Full-text filter over the JSON values (case-insensitive, optional)")
            @RequestParam(required = false) String q) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        Page<JsonDocument> result = service.list(safePage, safeSize, q);

        List<DocumentPage.Row> rows = result.getContent().stream()
                .map(doc -> new DocumentPage.Row(doc.getId(), doc.getData()))
                .toList();

        log.info("List documents page={} size={} q={} -> {} of {} rows", safePage, safeSize, q, rows.size(), result.getTotalElements());

        return new DocumentPage(
                rows,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext());
    }

    @GetMapping("/{id}/page")
    @Operation(summary = "Locate a document's page",
            description = "Returns the zero-based page index that contains the given document, "
                    + "using the same ordering as the listing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page index for the document"),
            @ApiResponse(responseCode = "404", description = "No document with this UUID")
    })
    public ResponseEntity<DocumentLocation> locate(
            @Parameter(description = "UUID of the document") @PathVariable UUID id,
            @Parameter(description = "Page size (1.." + MAX_PAGE_SIZE + ")") @RequestParam(defaultValue = "10") int size) {

        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);

        return service.pageOf(id, safeSize)
                .map(p -> {
                    log.info("Locate document {} -> page {} (size {})", id, p, safeSize);
                    return ResponseEntity.ok(new DocumentLocation(p, safeSize));
                })
                .orElseGet(() -> {
                    log.warn("Document {} not found for locate", id);
                    return ResponseEntity.notFound().build();
                });
    }
}