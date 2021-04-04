package com.csdm.adbooker.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import static com.csdm.adbooker.model.NewsItemParameters.NEWS_FEED_URL;

public class GettingRssEntriesCallable implements Callable<List<SyndEntry>> {

    @Override
    public List<SyndEntry> call() throws Exception {
        URL feedUrl = new URL(NEWS_FEED_URL);

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(feedUrl));

        List<SyndEntry> entries = feed.getEntries();
        entries.sort(Comparator.comparing(SyndEntry::getPublishedDate).reversed());
        return entries;
    }
}
