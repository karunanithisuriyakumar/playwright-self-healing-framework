package com.qa.selfheal;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reactive self-healing locator engine.
 *
 * On each find():
 *   1. If a locator is already on record for this element (either seeded
 *      from a previous run, or healed previously), try THAT one first.
 *   2. If it fails to resolve -- this is the actual "break" being detected,
 *      in real time -- search the fallback strategy list for a replacement.
 *   3. If a replacement resolves, persist it as the new locator of record.
 *      This is the "heal". Every future run will use it directly in step 1,
 *      without needing to fail first.
 */
public class SelfHealingLocator {

    private final Page page;
    private final LocatorRepository repository;
    private final Map<String, String> healingLog = new LinkedHashMap<>();

    public SelfHealingLocator(Page page, LocatorRepository repository) {
        this.page = page;
        this.repository = repository;
    }

    public Locator find(String elementName, List<NamedLocatorStrategy> fallbackStrategies) {
        Optional<NamedLocatorStrategy> remembered = repository.get(elementName);

        // Step 1: try the known/remembered locator first
        if (remembered.isPresent()) {
            Locator locator = tryResolve(remembered.get());
            if (locator != null) {
                healingLog.put(elementName, "OK (remembered): " + remembered.get().getDescription());
                return locator;
            }
            System.out.println("[REACTIVE HEALING] '" + elementName + "' BROKE -- remembered locator "
                    + remembered.get().getDescription() + " no longer resolves. Searching for a replacement...");
        }

        // Step 2: search fallback strategies for a working replacement
        for (NamedLocatorStrategy strategy : fallbackStrategies) {
            Locator locator = tryResolve(strategy);
            if (locator != null) {
                if (remembered.isPresent()) {
                    System.out.println("[REACTIVE HEALING] '" + elementName + "' HEALED: "
                            + remembered.get().getDescription() + "  -->  " + strategy.getDescription());
                    healingLog.put(elementName, "HEALED: " + remembered.get().getDescription()
                            + " -> " + strategy.getDescription());
                } else {
                    healingLog.put(elementName, "OK (seeded): " + strategy.getDescription());
                }
                // Step 3: persist the fix so future runs don't need to re-discover it
                repository.save(elementName, strategy);
                return locator;
            }
        }

        throw new RuntimeException("Self-healing locator exhausted all strategies for: " + elementName);
    }

    private Locator tryResolve(NamedLocatorStrategy strategy) {
        try {
            Locator locator = strategy.resolve(page);
            if (locator.count() > 0 && locator.first().isVisible()) {
                return locator.first();
            }
        } catch (Exception ignored) {
            // this strategy didn't resolve -- move on
        }
        return null;
    }

    public Map<String, String> getHealingLog() {
        return healingLog;
    }
}
