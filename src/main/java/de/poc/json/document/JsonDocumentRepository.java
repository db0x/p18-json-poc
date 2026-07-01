package de.poc.json.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JsonDocumentRepository extends JpaRepository<JsonDocument, UUID> {

    @Query(value = "SELECT * FROM json_documents WHERE data ->> :fieldName ILIKE '%' || :fieldValue || '%'", nativeQuery = true)
    List<JsonDocument> findByFieldValue(@Param("fieldName") String fieldName, @Param("fieldValue") String fieldValue);

    /**
     * Full-text-like search across all JSON values (not keys). Matches a document
     * when any value in its {@code data} object contains the search term
     * (case-insensitive). Ordered by id to match the plain listing.
     */
    @Query(value = "SELECT * FROM json_documents d"
            + " WHERE EXISTS (SELECT 1 FROM jsonb_each_text(d.data) e WHERE e.value ILIKE '%' || :q || '%')"
            + " ORDER BY d.id",
            countQuery = "SELECT count(*) FROM json_documents d"
                    + " WHERE EXISTS (SELECT 1 FROM jsonb_each_text(d.data) e WHERE e.value ILIKE '%' || :q || '%')",
            nativeQuery = true)
    Page<JsonDocument> searchValues(@Param("q") String q, Pageable pageable);

    @Query(value = "SELECT * FROM json_documents WHERE name ILIKE '%' || :value || '%'", nativeQuery = true)
    List<JsonDocument> findByName(@Param("value") String value);

    // number of documents ordered before the given id -> its position in the id-sorted listing
    long countByIdLessThan(UUID id);
}
