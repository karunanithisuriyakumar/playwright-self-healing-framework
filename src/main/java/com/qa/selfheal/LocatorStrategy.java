package com.qa.selfheal;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * A single way of finding an element. The self-healing locator will try
 * a ranked list of these in order until one actually resolves to a visible element.
 */
public interface LocatorStrategy {
    Locator resolve(Page page);

    /** Human-readable description, used for the healing log / report. */
    String getDescription();
}
