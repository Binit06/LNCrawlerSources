package com.halovoid.lncrawlersources

import com.halovoid.lncrawler.api.core.crawler.Crawler

class CrawlerSourceAggregator {
    fun getCrawlers(): List<Crawler> {
        return listOf(
            NovelBins()
        )
    }
}