package com.halovoid.lncrawlersources

import com.halovoid.lncrawlersources.api.core.crawler.Crawler

class CrawlerSourceAggregator {
    fun getCrawlers(): List<Crawler> {
        return listOf(
            NovelBins()
        )
    }
}