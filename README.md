# Reactive Self-Healing Locators with Playwright (Java)

A working demo of **reactive self-healing** test automation: a locator
breaks, the system detects the failure at runtime, searches for a working
replacement, and **persists that fix to disk** so future runs use the
healed locator directly instead of re-discovering it (or failing) again.

## Reactive vs. proactive fallback -- the distinction that matters

An earlier version of this project just tried a list of locator strategies
in order every single run. That's **proactive fallback** -- resilient, but
not truly "self-healing," because nothing ever actually failed and got
repaired; it just skipped past options that didn't work.

This version is different. Here's the real lifecycle:

1. **Run 1 (page v1):** No locator is on record for the login button yet.
   The primary `#login-btn` id locator works. It gets **seeded** into a
   JSON repository (`locator-repository.json`) as the locator of record.

2. **Run 2 (page v2 -- simulating a frontend refactor):** The repository
   still has the *old* `#login-btn` locator on record. The system tries it
   first -- **and it fails**. That failure is the actual "break" being
   detected in real time. Only then does it search the fallback strategies
   (css -> role -> text), find that the role-based locator still works, and
   **heal**: it overwrites the repository entry with the new locator and
   logs exactly what changed.

3. **Run 3 (simulating a later, separate test run):** A brand new browser
   page and a brand new `SelfHealingLocator` instance load the same broken
   page. Because the repository already has the healed locator on record
   from Run 2, it's used **immediately** -- no failure, no re-search. The
   system "remembers" the fix.

That persistence across runs -- via disk, not just in-memory during a
single execution -- is what makes this reactive rather than just resilient.

## What's in here

```
src/main/java/com/qa/selfheal/
  LocatorStrategy.java        -> interface: one way of finding an element
  NamedLocatorStrategy.java   -> a serializable strategy (id/css/role/text)
  LocatorRepository.java      -> JSON-file-backed persistence of healed locators
  SelfHealingLocator.java     -> engine: try remembered -> detect failure -> heal -> persist

src/test/java/com/qa/tests/
  SelfHealingLocatorTest.java -> one deterministic test walking through all 3 runs

src/test/resources/
  page_v1.html   -> original page, button has id="login-btn"
  page_v2.html   -> "after a frontend refactor" -- id AND class both changed
```

## Running it

```bash
mvn install
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps"
mvn test
```

Watch the console for lines like:

```
[REACTIVE HEALING] 'loginButton' BROKE -- remembered locator by id: #login-btn no longer resolves. Searching for a replacement...
[REACTIVE HEALING] 'loginButton' HEALED: by id: #login-btn  -->  by role: button (name="Log In")
```

A `locator-repository.json` file will appear in your project root after the
run -- open it to see the healed locator persisted as plain JSON. The test
resets this file at the start of each run so the demo is repeatable; in a
real CI pipeline you'd instead keep this file (or a database) *between*
runs, which is exactly what makes healing carry forward.

## How this compares to real tools (Healenium, Testim, mabl)

This project is intentionally simple so the *mechanism* is visible and
explainable. Production tools go further:

- **DOM similarity scoring** instead of a fixed candidate list -- they store
  a snapshot of the element's full attribute set and position, and search
  the live DOM for the closest match when the primary locator fails, rather
  than only trying a few pre-written alternates.
- **Healenium specifically targets Selenium WebDriver**, not Playwright --
  it works via a proxy that intercepts WebDriver calls. There's no
  equivalent open-source tool for Playwright yet, which is exactly why this
  project builds the pattern from scratch.
- **A real database** (Healenium uses Postgres) instead of a flat JSON file,
  for concurrent test runs and healing history/audit trails.

## How to extend this (good next steps for your portfolio)

- **Element fingerprinting**: instead of a fixed list of fallback
  strategies, capture the full attribute set of an element on first success
  (tag, classes, text, position) and use that to *search* the page for the
  closest match when the primary locator breaks -- this gets you much
  closer to how Healenium actually works.
- **CI integration (Azure Pipelines)**: commit the repository JSON between
  runs instead of resetting it, and add a pipeline step that flags a Slack
  message or build annotation whenever a `HEALED` event is logged, so a
  human still reviews and fixes the real root cause.
- **A Selenium + Healenium demo** as a companion project, since Healenium is
  the real open-source tool for the ecosystem you already know (Selenium).
  "I've used an established self-healing tool (Healenium/Selenium) and also
  built the reactive pattern myself (Playwright)" is a strong resume line.

## How to talk about this in interviews

"I built a reactive self-healing locator layer for Playwright: it detects
when a previously-working locator breaks, searches ranked fallback
strategies for a replacement, and persists the fix to disk so subsequent
runs use the healed locator directly instead of re-discovering it every
time." That's a concrete, verifiable, and technically accurate claim tied
to a repo you can walk someone through line by line.
