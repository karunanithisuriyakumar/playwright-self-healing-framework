package com.qa.selfheal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Persists healed locators to a JSON file on disk, so once an element has
 * healed once, future runs use the healed locator directly instead of
 * re-discovering it every single time.
 *
 * This is what turns "try several strategies every run" into genuine
 * reactive self-healing: a specific, known locator is attempted, its
 * failure is what triggers the search, and the fix is remembered.
 */
public class LocatorRepository {

    private final File storeFile;
    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private Map<String, NamedLocatorStrategy> store = new HashMap<>();

    public LocatorRepository(String filePath) {
        this.storeFile = new File(filePath);
        load();
    }

    public Optional<NamedLocatorStrategy> get(String elementName) {
        return Optional.ofNullable(store.get(elementName));
    }

    public void save(String elementName, NamedLocatorStrategy strategy) {
        store.put(elementName, strategy);
        persist();
    }

    /** Wipes the repository -- useful so a demo run always starts from a clean slate. */
    public void reset() {
        store.clear();
        if (storeFile.exists()) {
            storeFile.delete();
        }
    }

    private void load() {
        if (storeFile.exists()) {
            try {
                store = mapper.readValue(storeFile, mapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, NamedLocatorStrategy.class));
            } catch (IOException e) {
                store = new HashMap<>();
            }
        }
    }

    private void persist() {
        try {
            mapper.writeValue(storeFile, store);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist locator repository to " + storeFile, e);
        }
    }
}
