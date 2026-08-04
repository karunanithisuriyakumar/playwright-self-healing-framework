package com.qa.selfheal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * A locator strategy that can be serialized to/from JSON. This is what makes
 * healing REACTIVE instead of just "try a list of options every single time":
 * once a strategy is found to work, it gets written to disk and reloaded as
 * the primary strategy on the next run.
 */
public class NamedLocatorStrategy implements LocatorStrategy {

    public enum StrategyType { ID, CSS, ROLE, TEXT }

    private StrategyType type;
    private String value;      // id value, css selector, or text content
    private String roleName;   // only used when type == ROLE

    /** Required no-arg constructor for Jackson deserialization. */
    public NamedLocatorStrategy() {}

    @JsonCreator
    public NamedLocatorStrategy(
            @JsonProperty("type") StrategyType type,
            @JsonProperty("value") String value,
            @JsonProperty("roleName") String roleName) {
        this.type = type;
        this.value = value;
        this.roleName = roleName;
    }

    public static NamedLocatorStrategy byId(String id) {
        return new NamedLocatorStrategy(StrategyType.ID, id, null);
    }

    public static NamedLocatorStrategy byCss(String css) {
        return new NamedLocatorStrategy(StrategyType.CSS, css, null);
    }

    public static NamedLocatorStrategy byRole(String role, String accessibleName) {
        return new NamedLocatorStrategy(StrategyType.ROLE, role, accessibleName);
    }

    public static NamedLocatorStrategy byText(String text) {
        return new NamedLocatorStrategy(StrategyType.TEXT, text, null);
    }

    @Override
    public Locator resolve(Page page) {
        switch (type) {
            case ID:
                return page.locator("#" + value);
            case CSS:
                return page.locator(value);
            case ROLE:
                return page.getByRole(
                        AriaRole.valueOf(value.toUpperCase()),
                        new Page.GetByRoleOptions().setName(roleName)
                );
            case TEXT:
                return page.getByText(value);
            default:
                throw new IllegalStateException("Unknown strategy type: " + type);
        }
    }

    @Override
    public String getDescription() {
        switch (type) {
            case ID:
                return "by id: #" + value;
            case CSS:
                return "by css: " + value;
            case ROLE:
                return "by role: " + value + " (name=\"" + roleName + "\")";
            case TEXT:
                return "by text: \"" + value + "\"";
            default:
                return "unknown strategy";
        }
    }

    // getters/setters required for Jackson JSON (de)serialization
    public StrategyType getType() { return type; }
    public void setType(StrategyType type) { this.type = type; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
