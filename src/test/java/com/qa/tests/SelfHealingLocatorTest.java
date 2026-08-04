package com.qa.tests;

import com.microsoft.playwright.*;
import com.qa.selfheal.LocatorRepository;
import com.qa.selfheal.NamedLocatorStrategy;
import com.qa.selfheal.SelfHealingLocator;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelfHealingLocatorTest {

    static Playwright playwright;
    static Browser browser;
    Page page;
    LocatorRepository repository;

    private static final String REPO_FILE = "locator-repository.json";

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false)
                        .setSlowMo(400)
        );
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void setup() {
        page = browser.newPage();
        // start from a clean slate each run so the demo is repeatable
        repository = new LocatorRepository(REPO_FILE);
        repository.reset();
    }

    @AfterEach
    void closePage() {
        page.close();
    }

    @Test
    @DisplayName("Reactive healing: locator breaks, gets detected, healed, and remembered for next run")
    void demonstratesReactiveHealing() {
        SelfHealingLocator healer = new SelfHealingLocator(page, repository);

        List<NamedLocatorStrategy> strategies = List.of(
                NamedLocatorStrategy.byId("login-btn"),
                NamedLocatorStrategy.byCss("button.login"),
                NamedLocatorStrategy.byRole("button", "Log In"),
                NamedLocatorStrategy.byText("Log In")
        );

        // ---- RUN 1: page v1. No locator on record yet -> id locator works -> gets SEEDED ----
        page.navigate(fileUrl("page_v1.html"));
        Locator button1 = healer.find("loginButton", strategies);
        assertTrue(button1.isVisible());
        assertTrue(healer.getHealingLog().get("loginButton").startsWith("OK (seeded)"));
        assertEquals("by id: #login-btn", repository.get("loginButton").get().getDescription());

        // ---- RUN 2: page v2. id AND class both changed by a "frontend refactor". ----
        // The repository still has the OLD #login-btn locator on record.
        // This is the actual reactive moment: it tries the remembered
        // locator, that attempt FAILS, and only then does it search for
        // and persist a replacement.
        page.navigate(fileUrl("page_v2.html"));
        Locator button2 = healer.find("loginButton", strategies);
        assertTrue(button2.isVisible());
        assertTrue(healer.getHealingLog().get("loginButton").startsWith("HEALED"));
        assertEquals("by role: button (name=\"Log In\")", repository.get("loginButton").get().getDescription());

        // ---- RUN 3: brand new page + brand new SelfHealingLocator instance, ----
        // simulating a completely separate test run later on, same broken
        // site, same repository file on disk. It should use the HEALED
        // locator immediately -- no failure, no re-search needed this time.
        Page freshPage = browser.newPage();
        freshPage.navigate(fileUrl("page_v2.html"));
        SelfHealingLocator healerNextRun = new SelfHealingLocator(freshPage, repository);
        Locator button3 = healerNextRun.find("loginButton", strategies);
        assertTrue(button3.isVisible());
        assertTrue(healerNextRun.getHealingLog().get("loginButton").startsWith("OK (remembered)"));
        freshPage.close();
    }

    private String fileUrl(String resourceName) {
        return "file://" + Paths.get("src/test/resources", resourceName).toAbsolutePath();
    }
}
