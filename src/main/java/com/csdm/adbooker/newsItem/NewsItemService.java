package com.csdm.adbooker.newsItem;

import com.csdm.adbooker.newsItem.model.NewsItemDto;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.csdm.adbooker.newsItem.model.NewsItemParameters.RSS_FEED_INTERVAL_TIME;

@Slf4j
@EnableScheduling
@Service
public class NewsItemService {

    @Autowired
    private NewsItemSavingService savingService;

    @Autowired
    private NewsItemUpdatingService updatingService;

    @Autowired
    private RssFetcher rssFetcher;

    @Autowired
    private NewsItemConverter converter;

    @Scheduled(fixedRate = RSS_FEED_INTERVAL_TIME)
    @Transactional
    public void fetchRssFeeds() {
        log.info("Started fetching rss feeds at {}", LocalDateTime.now());

        List<SyndEntry> entries = rssFetcher.makeHttpRequestAndGetRssEntries();
        if (CollectionUtils.isEmpty(entries)) {
            return;
        }
        List<String> latestGuidsFromRssFeeds = converter.collectGuidsFrom(entries);

        List<NewsItemDto> newsItemsFromDb = converter.getNewsItemsFromDbBy(latestGuidsFromRssFeeds);
        List<NewsItemDto> newsItemsFromRss = converter.convertRssEntryToDto(entries);

        Map<String, NewsItemDto> newsItemsMapFromDb = converter.createMapByItemsFrom(newsItemsFromDb);
        Map<String, NewsItemDto> newsItemsMapFromRss = converter.createMapByItemsFrom(newsItemsFromRss);

        updatingService.updateItemsInDbByRssFeeds(latestGuidsFromRssFeeds, newsItemsMapFromDb, newsItemsMapFromRss);
        savingService.saveNewItemsInDb(latestGuidsFromRssFeeds, newsItemsMapFromDb, newsItemsMapFromRss);

        log.info("Finished fetching rss feeds at {}", LocalDateTime.now());
    }

}
