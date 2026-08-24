package com.halovoid.lncrawlersources.crawler

import android.util.Log
import com.halovoid.lncrawler.api.core.config.CrawlerConfig
import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import org.json.JSONArray
import org.jsoup.Jsoup
import java.io.IOException

/**
 * Crawler implementation for RoyalRoad (royalroad.com) in the Data layer.
 * This class handles extracting the novel details and parsing the embedded
 * window.chapters JSON object to bypass table pagination completely.
 */
class RoyalRoad : Crawler() {
    override val name: String = "Royal Road"
    override val baseUrl: String = "https://www.royalroad.com"
    override val webviewNeeded: Boolean = false

    override val config: CrawlerConfig
        get() = _config ?: CrawlerConfig(
            userFolderLocation = "",
            maxAttempts = 3,
            runnerConcurrency = 3
        )

    override val chapterPerVolume: Int = 50

    override fun canHandle(url: String): Boolean {
        return url.contains("royalroad.com") || url.contains("royalroadl.com")
    }

    override suspend fun getNovelDetails(novelUrl: String): Novel {
        val doc = getDocument(novelUrl) ?: throw IOException("Failed to fetch novel details from $novelUrl")
        Log.i(name, "Scraping novel: $novelUrl")

        val title = doc.selectFirst(".fic-header h1")?.text() ?: ""
        val author = doc.selectFirst(".fic-header h4 a")?.text() ?: ""

        var coverUrl = doc.selectFirst(".fic-header img.thumbnail")?.attr("abs:src") ?: ""
        // Optional: clear out the default no-cover image if preferred
        if (coverUrl.contains("nocover")) {
            coverUrl = ""
        }

        // RoyalRoad usually hides the full description in a child div
        val descriptionElement = doc.selectFirst(".description .hidden-content")
            ?: doc.selectFirst(".description")
        val description = descriptionElement?.html()?.trim() ?: ""

        val chapters = mutableListOf<Chapter>()

        // The site populates its paginated table using a JSON array in the script tags.
        // We can extract this directly to get all chapters without making AJAX pagination requests.
        val scriptContent = doc.getElementsByTag("script").map { it.html() }.find { it.contains("window.chapters") }

        if (scriptContent != null) {
            try {
                val regex = Regex("""window\.chapters\s*=\s*(\[.*?\]);""")
                val matchResult = regex.find(scriptContent)
                if (matchResult != null) {
                    val jsonArray = JSONArray(matchResult.groupValues[1])
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val chapTitle = obj.getString("title")
                        val chapUrl = obj.getString("url")

                        val finalUrl = if (chapUrl.startsWith("http")) chapUrl else "$baseUrl$chapUrl"

                        chapters.add(
                            Chapter(
                                id = 0,
                                url = finalUrl,
                                novelUrl = novelUrl,
                                title = chapTitle,
                                index = i + 1,
                                volumeId = "${novelUrl}_vol_${(i / chapterPerVolume) + 1}",
                                fileLocation = null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(name, "Error parsing window.chapters JSON", e)
            }
        }

        // Fallback in case they change their site architecture and remove the JSON array
        if (chapters.isEmpty()) {
            doc.select("#chapters tbody tr.chapter-row").forEachIndexed { index, element ->
                val link = element.selectFirst("a[href]")
                if (link != null) {
                    chapters.add(
                        Chapter(
                            id = 0,
                            url = link.attr("abs:href"),
                            novelUrl = novelUrl,
                            title = link.text(),
                            index = index + 1,
                            volumeId = "${novelUrl}_vol_${(index / chapterPerVolume) + 1}",
                            fileLocation = null
                        )
                    )
                }
            }
        }

        return prepareNovel(
            Novel(
                url = novelUrl,
                title = title,
                author = author,
                coverUrl = coverUrl,
                description = description,
                chapters = chapters,
                crawlerName = name,
                alternativeNames = "" // RoyalRoad rarely uses alt names, usually tags instead
            )
        )
    }

    override suspend fun getChapterContent(chapterUrl: String): String? {
        val doc = getDocument(chapterUrl) ?: return null
        Log.i(name, "Scraping chapter: $chapterUrl")

        // Grab the main content container
        val content = cleanHtml(doc, ".chapter-content")

        // RoyalRoad injects randomized anti-piracy spans directly into the text paragraphs.
        // We parse the cleanHtml result to locate and remove these specific warning blocks.
        return Jsoup.parse(content).apply {
            select("span").forEach { span ->
                val text = span.text()
                if (text.contains("Royal Road is the home of this novel", ignoreCase = true) ||
                    text.contains("support the author", ignoreCase = true) ||
                    text.contains("stolen from", ignoreCase = true)) {
                    span.remove()
                }
            }
        }.body().html().trim()
    }
}