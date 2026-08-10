package com.halovoid.lncrawlersources

import com.halovoid.lncrawler.api.core.crawler.Crawler

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