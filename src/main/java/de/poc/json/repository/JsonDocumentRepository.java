package de.poc.json.repository;

import de.poc.json.entity.JsonDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JsonDocumentRepository extends JpaRepository<JsonDocument, UUID> {

    @Query(value = "SELECT * FROM json_documents WHERE data ->> :fieldName ILIKE '%' || :fieldValue || '%'", nativeQuery = true)
    List<JsonDocument> findByFieldValue(@Param("fieldName") String fieldName, @Param("fieldValue") String fieldValue);

    @Query(value = "SELECT * FROM json_documents WHERE name ILIKE '%' || :value || '%'", nativeQuery = true)
    List<JsonDocument> findByName(@Param("value") String value);
}
