package com.csdm.adbooker.service;

import com.csdm.adbooker.model.NewsItem;
import com.csdm.adbooker.model.NewsItemDto;
import com.csdm.adbooker.repository.NewsItemRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@EnableScheduling
@Service
public class NewsItemService {

    private Logger logger = LoggerFactory.getLogger(NewsItemService.class);
    private static final String NEWS_FEED_URL = "http://feeds.nos.nl/nosjournaal?format=xml";

    private final NewsItemRepository repository;

    public NewsItemService(NewsItemRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedRate = 2000)
    @Transactional
    public void fetchRssFeeds() {
        logger.info("Fetching rss feeds..");
        try {
            URL feedUrl = new URL(NEWS_FEED_URL);

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));

            List<SyndEntry> entries = feed.getEntries();

            List<String> guids = entries.stream().map(SyndEntry::getUri).collect(Collectors.toList());
            List<NewsItemDto> newsItemsFromDatabase = guids.stream()
                    .filter(item -> repository.findByGuid(item) != null).map(getFromDatabaseConvertToDto()).collect(Collectors.toList());
            List<NewsItemDto> newsItemsFromRss = entries.stream().map(entry -> NewsItemDto.builder()
                    .title(entry.getTitle())
                    .description(entry.getDescription().getValue())
                    .imageUrl(entry.getEnclosures().get(0).getUrl())
                    .publishedDate(feed.getPublishedDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime())
                    .guid(entry.getUri())
                    .build()).collect(Collectors.toList());

            Map<String, NewsItemDto> newsItemsMapFromDb = newsItemsFromDatabase.stream().collect(Collectors.toMap(NewsItemDto::getGuid, Function.identity()));
            Map<String, NewsItemDto> newsItemsMapFromRss = newsItemsFromRss.stream().collect(Collectors.toMap(NewsItemDto::getGuid, Function.identity()));

            List<NewsItem> entities = new ArrayList<>();
            for (String guidFromRss : newsItemsMapFromRss.keySet()) {
                NewsItemDto newsItemFromDb = newsItemsMapFromDb.get(guidFromRss);
                NewsItemDto newsItemFromRss = newsItemsMapFromRss.get(guidFromRss);
                if (newsItemFromDb == null) {
                    // new item
                    entities.add(NewsItem.builder().title(newsItemFromRss.getTitle())
                            .guid(newsItemFromRss.getGuid())
                            .description(newsItemFromRss.getDescription())
                            .publishedDate(newsItemFromRss.getPublishedDate())
                            .imageUrl(newsItemFromRss.getImageUrl()).build());
                } else {
                    // update item
                    NewsItem willBeUpdated = repository.findByGuid(newsItemFromDb.getGuid());
                    if (!newsItemFromDb.getTitle().equals(newsItemFromRss.getTitle())) {
                        willBeUpdated.setTitle(newsItemFromRss.getTitle());
                    }
                    if (!newsItemFromDb.getDescription().equals(newsItemFromRss.getDescription())) {
                        willBeUpdated.setDescription(newsItemFromRss.getDescription());
                    }
                    if (!newsItemFromDb.getPublishedDate().equals(newsItemFromRss.getPublishedDate())) {
                        willBeUpdated.setPublishedDate(newsItemFromRss.getPublishedDate());
                    }
                    if (!newsItemFromDb.getImageUrl().equals(newsItemFromRss.getImageUrl())) {
                        willBeUpdated.setImageUrl(newsItemFromRss.getImageUrl());
                    }
                }
            }
            repository.saveAll(entities);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private Function<String, NewsItemDto> getFromDatabaseConvertToDto() {
        return guid -> {
            NewsItem existingEntity = repository.findByGuid(guid);
            NewsItemDto existingNewsItem = NewsItemDto
                    .builder()
                    .guid(existingEntity.getGuid())
                    .title(existingEntity.getTitle())
                    .description(existingEntity.getDescription())
                    .publishedDate(existingEntity.getPublishedDate())
                    .imageUrl(existingEntity.getImageUrl())
                    .build();
            return existingNewsItem;
        };
    }
}
