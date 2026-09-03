package com.halovoid.lncrawlersources.crawler

import android.util.Log
import com.halovoid.lncrawler.api.core.config.CrawlerConfig
import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Crawler implementation for Novel Archive (novelarchive.cc).
 * This crawler uses the site's JSON API for searching, metadata, and chapter content.
 */
class NovelArchive : Crawler() {
    override val name: String = "Novel Archive"
    override val baseUrl: String = "https://novelarchive.cc"
    override val webviewNeeded: Boolean = false

    override val config: CrawlerConfig
        get() = _config ?: CrawlerConfig(
            userFolderLocation = "",
            maxAttempts = 3,
            runnerConcurrency = 1 // Slower scraping as requested
        )

    override val chapterPerVolume: Int = 100

    override fun canHandle(url: String): Boolean {
        return url.contains("novelarchive.cc")
    }

    override suspend fun getNovelMetadata(novelUrl: String): Novel {
        val novelId = extractId(novelUrl) ?: throw IOException("Could not extract novel ID from $novelUrl")
        val apiUrl = "$baseUrl/api/novels/$novelId"
        
        val jsonString = fetchHtml(apiUrl) ?: throw IOException("Failed to fetch novel metadata from $apiUrl")
        Log.i(name, "Scraping novel metadata: $apiUrl")

        val json = JSONObject(jsonString).getJSONObject("novel")
        val title = json.optString("title")
        val author = json.optString("author")
        val coverUrl = json.optString("cover_url").let { if (it.startsWith("/")) "$baseUrl$it" else it }
        val description = json.optString("description")
        
        // We store the preferred source in the novel's preferred_source field if it exists
        // However, Novel model doesn't have it, so we might need to "hide" it in URL or metadata if needed.
        // Actually, we can just fetch it again in getChapterList.

        return Novel(
            url = novelUrl,
            title = title,
            author = author,
            coverUrl = coverUrl,
            description = description,
            chapters = emptyList(),
            crawlerName = name,
            alternativeNames = json.optJSONArray("associated_names")?.let { arr ->
                List(arr.length()) { arr.getString(it) }.joinToString(", ")
            }
        )
    }

    override suspend fun getChapterList(novelUrl: String): List<Chapter> {
        val novelId = extractId(novelUrl) ?: throw IOException("Could not extract novel ID from $novelUrl")
        val novelApiUrl = "$baseUrl/api/novels/$novelId"
        
        val novelJsonString = fetchHtml(novelApiUrl) ?: throw IOException("Failed to fetch novel metadata from $novelApiUrl")
        val novelJson = JSONObject(novelJsonString).getJSONObject("novel")
        
        val candidateSources = mutableListOf<String>()
        val preferredSource = novelJson.optString("preferred_source").trim()
        if (preferredSource.isNotEmpty()) {
            candidateSources.add(preferredSource)
        }

        // Fetch available sources if preferred_source is empty or fails
        val sourcesApiUrl = "$baseUrl/api/novels/$novelId/sources"
        val sourcesJsonString = fetchHtml(sourcesApiUrl)
        if (sourcesJsonString != null) {
            val sourcesJson = JSONObject(sourcesJsonString)
            val sourcesArray = sourcesJson.optJSONArray("sources")
            if (sourcesArray != null) {
                for (i in 0 until sourcesArray.length()) {
                    val srcId = sourcesArray.getJSONObject(i).optString("id").trim()
                    if (srcId.isNotEmpty() && !candidateSources.contains(srcId)) {
                        candidateSources.add(srcId)
                    }
                }
            }
        }

        // Add common fallback sources if not present
        for (fallback in listOf("fucknovelpia", "ranobes")) {
            if (!candidateSources.contains(fallback)) {
                candidateSources.add(fallback)
            }
        }

        var selectedSource = ""
        var chaptersArray: JSONArray? = null

        // Iterate through candidate sources to find one with non-empty chapters
        for (source in candidateSources) {
            val chaptersApiUrl = "$baseUrl/api/novels/$novelId/sources/$source/chapters"
            val chaptersJsonString = fetchHtml(chaptersApiUrl) ?: continue
            Log.i(name, "Scraping chapter list: $chaptersApiUrl")

            try {
                val chaptersJson = JSONObject(chaptersJsonString)
                val arr = chaptersJson.optJSONArray("chapters")
                if (arr != null && arr.length() > 0) {
                    selectedSource = source
                    chaptersArray = arr
                    break
                }
            } catch (e: Exception) {
                Log.w(name, "Failed to parse chapter list for source $source", e)
            }
        }

        if (chaptersArray == null || selectedSource.isEmpty()) {
            Log.w(name, "No chapters found for any source of novel $novelId")
            return emptyList()
        }

        val chapters = mutableListOf<Chapter>()
        for (i in 0 until chaptersArray.length()) {
            val chapterObj = chaptersArray.getJSONObject(i)
            val number = chapterObj.getInt("number")
            val title = chapterObj.optString("title", "Chapter $number")
            
            chapters.add(
                Chapter(
                    id = 0,
                    // Store source and number in URL to be used in getChapterContent
                    url = "$baseUrl/api/novels/$novelId/sources/$selectedSource/chapters/$number",
                    novelUrl = novelUrl,
                    title = title,
                    index = i + 1,
                    volumeId = "${novelUrl}_vol_${(i / chapterPerVolume) + 1}",
                    fileLocation = null
                )
            )
        }

        return chapters
    }

    override suspend fun getNovelDetails(novelUrl: String): Novel {
        val metadata = getNovelMetadata(novelUrl)
        val chapters = getChapterList(novelUrl)
        return prepareNovel(metadata.copy(chapters = chapters))
    }

    override suspend fun getChapterContent(chapterUrl: String): String? {
        val jsonString = fetchHtml(chapterUrl) ?: return null
        Log.i(name, "Scraping chapter: $chapterUrl")

        val json = JSONObject(jsonString)
        val contentHtml = json.optString("content_html")
        if (contentHtml.isEmpty()) return null

        return contentHtml
            .replace("src=\"/", "src=\"$baseUrl/")
            .replace("src='/", "src='$baseUrl/")
    }

    override suspend fun getSearchResults(query: String): List<Novel> {
        val searchUrl = "$baseUrl/api/novels?search=${query.replace(" ", "+")}&fuzzy=1"
        val jsonString = fetchHtml(searchUrl) ?: return emptyList()

        val json = JSONObject(jsonString)
        val novelsArray = json.getJSONArray("novels")
        val results = mutableListOf<Novel>()

        for (i in 0 until novelsArray.length()) {
            val novelObj = novelsArray.getJSONObject(i)
            val id = novelObj.getString("id")
            val title = novelObj.getString("title")
            val author = novelObj.optString("author")
            val coverUrl = novelObj.optString("cover_url").let { if (it.startsWith("/")) "$baseUrl$it" else it }

            results.add(
                Novel(
                    url = "$baseUrl/novel?id=$id",
                    title = title,
                    author = author,
                    coverUrl = coverUrl,
                    description = novelObj.optString("description"),
                    chapters = emptyList(),
                    crawlerName = name,
                    alternativeNames = null
                )
            )
        }
        return results
    }

    private fun extractId(url: String): String? {
        // Handle https://novelarchive.cc/novel?id=6a69a7ed0a8005dd9415f190
        if (url.contains("id=")) {
            return url.substringAfter("id=").substringBefore("&")
        }
        // Handle https://novelarchive.cc/api/novels/6a69a7ed0a8005dd9415f190
        val pathSegments = url.split("/")
        if (pathSegments.lastOrNull()?.length == 24) { // Typical MongoDB ObjectId length
            return pathSegments.last()
        }
        return null
    }
}
