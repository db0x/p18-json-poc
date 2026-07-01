package de.poc.json.document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One page of documents for tabular display, plus the metadata a client needs
 * to load the next page.
 */
public record DocumentPage(
        List<Row> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public record Row(UUID id, Map<String, Object> data) {
    }
}