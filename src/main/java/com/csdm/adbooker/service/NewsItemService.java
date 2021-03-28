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
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.ZoneId;
import java.util.List;
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

    //@Scheduled(fixedRate = 2000)
    public void fetchRssFeeds() {
        logger.info("Fetching rss feeds..");
        try {
            URL feedUrl = new URL(NEWS_FEED_URL);

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));

            List<SyndEntry> entries = feed.getEntries();

            List<String> guids = entries.stream().map(SyndEntry::getUri).collect(Collectors.toList());
            List<NewsItemDto> existingNewsItems = guids.stream().map(getFromDatabaseConvertToDto()).collect(Collectors.toList());

            List<NewsItem> news = entries.stream().map(entry -> NewsItem.builder()
                    .title(entry.getTitle())
                    .description(entry.getDescription().getValue())
                    .imageUrl(entry.getEnclosures().get(0).getUrl())
                    .publishedDate(feed.getPublishedDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime())
                    .guid(entry.getUri())
                    .build()).collect(Collectors.toList());
            repository.saveAll(news);
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
