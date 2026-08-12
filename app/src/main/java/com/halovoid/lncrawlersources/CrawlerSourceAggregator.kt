package com.halovoid.lncrawlersources

import com.halovoid.lncrawler.api.core.crawler.Crawler
import com.halovoid.lncrawlersources.crawler.NovelBins
import com.halovoid.lncrawlersources.crawler.NovelFull
import com.halovoid.lncrawlersources.crawler.Novgo

class CrawlerSourceAggregator {
    fun getCrawlers(): List<Crawler> {
        return listOf(
            NovelBins(),
            NovelFull(),
            Novgo(),
        )
    }

    fun getMinAppVersion(): String {
        return "1.0.0"
    }
}