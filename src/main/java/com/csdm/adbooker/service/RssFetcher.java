package com.csdm.adbooker.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Comparator;
import java.util.List;

import static com.csdm.adbooker.model.NewsItemParameters.NEWS_FEED_URL;

@Slf4j
@Service
public class RssFetcher {

    public List<SyndEntry> makeHttpRequestAndGetRssEntries() {
        List<SyndEntry> entries = null;
        try {
            URL feedUrl = new URL(NEWS_FEED_URL);

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = null;
            feed = input.build(new XmlReader(feedUrl));

            entries = feed.getEntries();
            entries.sort(Comparator.comparing(SyndEntry::getPublishedDate).reversed());
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return entries;
    }
}
