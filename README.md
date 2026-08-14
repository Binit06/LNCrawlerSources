# LNCrawlerSources

Crawler implementations for [LNCrawler](https://github.com/Binit06/LNCrawler).

This repository contains source-specific crawler implementations that are packaged and loaded by LNCrawler. Each crawler is responsible for handling a particular novel website, while the core crawling functionality is provided by the LNCrawler application.

## How It Works

LNCrawlerSources is intentionally kept lightweight. A source package consists of two main parts:

- **CrawlerSourceAggregator**: 
    - Registers available crawlers.
    - Defines the minimum supported LNCrawler version.
- **Crawlers**: 
    - Individual implementations for each source (e.g., NovelBins).

### CrawlerSourceAggregator

`CrawlerSourceAggregator` acts as the entry point for the source package. It provides LNCrawler with:
1. The list of crawler implementations included in the package.
2. The minimum LNCrawler version required by the crawlers.

Example:
```kotlin
class CrawlerSourceAggregator {

    fun getCrawlers(): List<Crawler> {
        return listOf(
            NovelBins()
        )
    }

    fun getMinAppVersion(): String {
        return "1.0.0"
    }
}
```
When adding a crawler, it must be registered in `getCrawlers()`.

### Adding a Crawler

Each crawler extends the `Crawler` class provided by LNCrawler. A crawler is responsible for implementing source-specific logic, such as:
- Detecting whether a URL belongs to the source.
- Extracting novel metadata.
- Extracting chapters.
- Fetching chapter content.
- Handling source-specific APIs or pagination.
- Cleaning source-specific HTML.

Example:
```kotlin
class NovelBins : Crawler() {

    override val name = "NovelBins"
    override val baseUrl = "https://novelbins.com"

    override fun canHandle(url: String): Boolean {
        return url.contains("novelbins.com") ||
               url.contains("novelbin.com")
    }

    // ...
}
```
> [!TIP]
> Keep source-specific behavior inside the crawler. Avoid modifying the shared LNCrawler API or core crawler infrastructure when the behavior only applies to one source.

## LNCrawler API

Crawler implementations depend on the API provided by LNCrawler. The dependency is declared in `build.gradle` as:

```gradle
compileOnly("com.github.Binit06:LNCrawler:v1.0.1")
```

The dependency is `compileOnly` because LNCrawlerSources is compiled against the LNCrawler API, while the actual LNCrawler application provides the runtime implementation when the crawler package is loaded. **Do not package another copy of LNCrawler inside the source bundle.**

## Minimum App Version

`CrawlerSourceAggregator.getMinAppVersion()` specifies the minimum LNCrawler version required by the included crawlers. If a crawler starts using API functionality that is unavailable in older versions of LNCrawler, update this value accordingly.

```kotlin
fun getMinAppVersion(): String {
    return "1.0.0"
}
```

## Testing

A crawler can be tested locally without immediately publishing it to this repository's source package.

1. Temporarily add the crawler to your local LNCrawler project.
2. Register it in `CrawlerFactory`. 
3. LNCrawler supports loading built-in crawlers, making this useful for development and debugging.

Once the crawler has been tested successfully, move the implementation into LNCrawlerSources and register it through `CrawlerSourceAggregator`.

## Supported Sources

| Source            | Website                                      |
|:------------------|:---------------------------------------------|
| **Novel Bins**    | [novelbins.com](https://novelbins.com)       |
| **Novel Full**    | [novelfull.com](https://novelfull.com)       |
| **Nov Go**        | [novgo.net](https://novgo.net)               |
| **Novel Phoenix** | [novelphoenix.com](https://novelphoenix.com) |
| **Novel Fire**    | [novelfire.net](https://novelfire.net)    |

## Requirements

- Android Studio
- Kotlin
- Gradle
- A compatible LNCrawler API version

## Contributing

Want to add a new source or improve an existing crawler? See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

See [LICENSE](LICENSE) for the license applicable to this repository.
