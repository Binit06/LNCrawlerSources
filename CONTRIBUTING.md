# Contributing to LNCrawlerSources

Thank you for contributing to LNCrawlerSources! 

This repository contains the crawler implementations used by [LNCrawler](https://github.com/Binit06/LNCrawler). The most valuable contribution you can make here is adding support for a new novel source or improving an existing crawler.

## What You Can Contribute

- Add a new novel source.
- Fix an existing crawler.
- Improve novel or chapter parsing.
- Handle changes to a source's website.
- Improve source-specific error handling.
- Improve crawler documentation.

> [!NOTE]
> For changes to the core crawler API or LNCrawler application, please contribute to the [LNCrawler](https://github.com/Binit06/LNCrawler) repository instead.

---

## Getting Started

1. **Fork the repository** and clone your fork:
   ```bash
   git clone https://github.com/<your-username>/LNCrawlerSources.git
   cd LNCrawlerSources
   ```
2. **Open the project** in Android Studio and allow Gradle to synchronize.
3. **Verify Dependencies**: The project depends on the LNCrawler API:
   ```kotlin
   compileOnly("com.github.Binit06:LNCrawler:v1.0.1")
   ```
   Make sure the API version you're developing against provides everything your crawler requires.

---

## Adding a New Crawler

Create a new class that extends `Crawler`. A crawler should implement the source-specific behavior required by LNCrawler.

### Minimum Implementation Example

```kotlin
class MySource : Crawler() {

    override val name: String = "My Source"

    override val baseUrl: String = "https://example.com"

    override fun canHandle(url: String): Boolean {
        return url.contains("example.com")
    }

    override suspend fun getNovelDetails(novelUrl: String): Novel {
        // ... implementation
    }

    override suspend fun getChapterContent(chapterUrl: String): String? {
        // ... implementation
    }
}
```

### Register the Crawler

After creating the crawler, add it to `CrawlerSourceAggregator.kt`:

```kotlin
fun getCrawlers(): List<Crawler> {
    return listOf(
        NovelBins(),
        MySource() // Add your new source here
    )
}
```
If the crawler is not registered here, LNCrawler will not discover it from the source package.

---

## Testing a Crawler

You do not need to add an unfinished crawler to the source package while developing it. For local testing, you can temporarily place the crawler implementation directly in your own fork of LNCrawler and register it with `CrawlerFactory`.

LNCrawler supports built-in crawlers, which allows you to:
- Debug the crawler directly.
- Test it against real URLs.
- Iterate without rebuilding the source package.
- Verify compatibility with the current LNCrawler API.

Once the crawler is ready, move it into LNCrawlerSources and register it through `CrawlerSourceAggregator`.

> [!IMPORTANT]
> **Do not leave development-only crawlers in the LNCrawler application.** Once a crawler is ready for contribution, its implementation should be added to LNCrawlerSources.

---

## Source-Specific Logic

Keep all logic that exists because of a particular website **inside that crawler**. This includes:
- CSS selectors and URL patterns.
- HTML parsing and cleaning logic.
- AJAX/API requests and pagination.
- Website-specific workarounds or headers.

*Example: If a website requires a special AJAX request to retrieve chapters, implement that request inside the corresponding crawler rather than adding special handling to the shared infrastructure.*

---

## Handling Website Changes

Novel websites often change their structure without notice. When fixing a crawler:
1. Identify what exactly changed on the source.
2. Update only the affected source-specific logic where possible.
3. **Test thoroughly**: Verify metadata extraction, chapter discovery, and content extraction across multiple novels.

---

## API Compatibility

If your crawler requires a newer API version, ensure the minimum application version in `CrawlerSourceAggregator` is updated:

```kotlin
fun getMinAppVersion(): String {
    return "1.1.0" // Update this as needed
}
```

> [!WARNING]
> Do not use API functionality that is unavailable in the minimum supported application version.

---

## Crawler Quality Checklist

Before submitting a crawler, verify:
- [ ] **Novel Metadata**: Cover image, description, author, and alternative names.
- [ ] **Chapter Discovery**: Correctly finds all chapters, including those on multiple pages.
- [ ] **Chapter Content**: Clean text, no ads, correct encoding.
- [ ] **Edge Cases**: Handles missing metadata or unusual URLs gracefully without crashing.

---

## Pull Requests

Before opening a pull request:
- Make sure the project builds successfully.
- Remove debugging code and unnecessary logs.
- Ensure the crawler is registered in `CrawlerSourceAggregator`.
- Keep unrelated changes out of the PR.

**In the PR description, include:**
- Source name and URL.
- What was implemented/fixed.
- How the crawler was tested.

---

## Commit Messages

Keep commit messages concise and descriptive (following [Conventional Commits](https://www.conventionalcommits.org/) is preferred):

**Good Examples:**
```text
feat: add NovelBins crawler
fix: handle missing novel description
fix: update chapter pagination
refactor: simplify chapter parsing
```

**Avoid:** `update`, `changes`, `fix`, `stuff`.

---

## Reporting Issues

If an existing crawler is broken, open an issue including:
- Source name and Novel/Chapter URL.
- Expected vs. Actual behavior.
- Relevant logs or error messages.

---

## Code Guidelines

- **Focus**: Keep functions focused and avoids unnecessary abstractions.
- **Resilience**: Handle network and parsing failures gracefully.
- **Cleanliness**: Remove debugging code before submission.

---

## Thank You!

Every supported source makes LNCrawler more useful. Thank you for helping expand the ecosystem!
