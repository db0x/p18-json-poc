package de.poc.json.document;

/**
 * Where a single document sits in the paginated listing: the zero-based page
 * index for the given page size.
 */
public record DocumentLocation(int page, int size) {
}
