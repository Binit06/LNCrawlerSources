package com.halovoid.lncrawlersources.crawler

import android.util.Log
import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import org.jsoup.Jsoup
import java.io.IOException

/**
 * Crawler implementation for Novel Phoenix (novelphoenix.com) in the Data layer.
 * This class handles the specific HTML structure of the site, utilizing Jsoup
 * for static parsing and iterating through paginated chapter lists.
 */
class NovelPhoenix : Crawler() {
    override val name: String = "Novel Phoenix"
    override val baseUrl: String = "https://novelphoenix.com"

    override val chapterPerVolume: Int = 100 // Adjust as needed for local app logic

    override fun canHandle(url: String): Boolean {
        return url.contains("novelphoenix.com")
    }

    override suspend fun getNovelDetails(novelUrl: String): Novel {
        val cleanNovelUrl = novelUrl.removeSuffix("/")
        val doc = getDocument(cleanNovelUrl) ?: throw IOException("Failed to fetch novel details from $novelUrl")
        Log.i(name, "Scraping novel: $cleanNovelUrl")

        val title = doc.select("h1.novel-title").text().trim()
        val author = doc.select(".author a[itemprop='author'], .author span[itemprop='author']").text().trim()
        val coverUrl = doc.select(".fixed-img figure.cover img").attr("abs:src")

        // Extracting description, skipping the "Show More" button text
        val description = doc.select(".summary .content").clone().apply {
            select(".expand").remove()
        }.text().trim()

        val chapters = mutableListOf<Chapter>()

        // Novel Phoenix hosts chapters on a separate subpage: /chapters
        val chapterListUrl = "$cleanNovelUrl/chapters"
        val chaptersDoc = getDocument(chapterListUrl)

        if (chaptersDoc != null) {
            // Determine maximum pages for pagination
            var maxPage = 1
            chaptersDoc.select("ul.pagination li.page-item a.page-link").forEach { element ->
                val pageUrl = element.attr("href")
                val pageNum = pageUrl.substringAfter("page=").substringBefore("&").toIntOrNull()
                if (pageNum != null && pageNum > maxPage) {
                    maxPage = pageNum
                }
            }

            Log.i(name, "Found $maxPage chapter page(s) for $cleanNovelUrl")

            // Loop through all pages to gather chapters
            for (i in 1..maxPage) {
                val pageDoc = if (i == 1) chaptersDoc else getDocument("$chapterListUrl?page=$i")

                pageDoc?.select("ul.chapter-list li a")?.forEach { element ->
                    val chapUrl = element.attr("abs:href")
                    // Use the strong tag for title if available, fallback to whole anchor text
                    val chapTitle = element.select("strong.chapter-title").text().ifEmpty {
                        element.text().replace(element.select(".chapter-no").text(), "").trim()
                    }

                    chapters.add(
                        Chapter(
                            id = 0,
                            url = chapUrl,
                            novelUrl = cleanNovelUrl,
                            title = chapTitle,
                            index = 0,      // Placeholder - recalculated in prepareNovel
                            volumeId = "",  // Placeholder - recalculated in prepareNovel
                            fileLocation = null
                        )
                    )
                }
            }
        } else {
            Log.e(name, "Could not fetch chapter list from $chapterListUrl")
        }

        // Clean up duplicate entries and set final indices
        val finalChapters = chapters.distinctBy { it.url }.mapIndexed { index, chapter ->
            chapter.copy(
                index = index + 1,
                volumeId = "${cleanNovelUrl}_vol_${(index / chapterPerVolume) + 1}"
            )
        }

        return prepareNovel(
            Novel(
                url = cleanNovelUrl,
                title = title,
                author = author,
                coverUrl = coverUrl,
                description = description,
                chapters = finalChapters,
                crawlerName = name,
                alternativeNames = null // Site doesn't seem to have prominent alternative names
            )
        )
    }

    override suspend fun getChapterContent(chapterUrl: String): String? {
        val doc = getDocument(chapterUrl) ?: return null
        Log.i(name, "Scraping chapter: $chapterUrl")

        // The site uses #content inside #chapter-container
        val content = cleanHtml(doc, "#content")

        // Extra site-specific cleaning to remove unwanted ads/scripts disguised as text
        return Jsoup.parse(content).apply {
            select("script, style, iframe, .nf-ads, .box-notice, .box-notification").remove()
        }.body().html().trim()
    }
}