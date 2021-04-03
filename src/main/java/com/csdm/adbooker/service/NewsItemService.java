package com.csdm.adbooker.service;

import com.csdm.adbooker.model.NewsItem;
import com.csdm.adbooker.model.NewsItemDto;
import com.csdm.adbooker.repository.NewsItemRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.csdm.adbooker.model.NewsItemParameters.RSS_FEED_INTERVAL_TIME;
import static com.csdm.adbooker.model.NewsItemParameters.RSS_FEED_SIZE_THRESHOLD;

@Slf4j
@EnableScheduling
@Service
public class NewsItemService {

    @Autowired
    private NewsItemSavingService savingService;

    @Autowired
    private NewsItemUpdatingService updatingService;

    @Autowired
    private NewsItemRepository repository;

    @Autowired
    private RssFetcher rssFetcher;

    @Scheduled(fixedRate = RSS_FEED_INTERVAL_TIME)
    @Transactional
    public void fetchRssFeeds() {
        log.info("Started fetching rss feeds at {}", LocalDateTime.now());

        List<SyndEntry> entries = rssFetcher.makeHttpRequestAndGetRssEntries();
        List<String> latestGuidsFromRssFeeds = collectGuidsFrom(entries);

        List<NewsItemDto> newsItemsFromDb = getNewsItemsFromDbBy(latestGuidsFromRssFeeds);
        List<NewsItemDto> newsItemsFromRss = convertRssEntryToDto(entries);

        Map<String, NewsItemDto> newsItemsMapFromDb = createMapByItemsFrom(newsItemsFromDb);
        Map<String, NewsItemDto> newsItemsMapFromRss = createMapByItemsFrom(newsItemsFromRss);

        updatingService.updateItemsInDbByRssFeeds(latestGuidsFromRssFeeds, newsItemsMapFromDb, newsItemsMapFromRss);
        savingService.saveNewItemsInDb(latestGuidsFromRssFeeds, newsItemsMapFromDb, newsItemsMapFromRss);

        log.info("Finished fetching rss feeds at {}", LocalDateTime.now());
    }

    private Map<String, NewsItemDto> createMapByItemsFrom(List<NewsItemDto> newsItemsFromDb) {
        return newsItemsFromDb.stream()
                .collect(Collectors.toMap(
                        NewsItemDto::getGuid,
                        Function.identity()));
    }

    @NotNull
    private List<NewsItemDto> convertRssEntryToDto(List<SyndEntry> entries) {
        return entries.stream()
                .limit(RSS_FEED_SIZE_THRESHOLD)
                .map(entry -> NewsItemDto.builder()
                        .title(entry.getTitle())
                        .description(entry.getDescription().getValue())
                        .imageUrl(entry.getEnclosures().get(0).getUrl())
                        .publishedDate(entry.getPublishedDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime())
                        .guid(entry.getUri())
                        .build()).collect(Collectors.toList());
    }

    @NotNull
    private List<NewsItemDto> getNewsItemsFromDbBy(List<String> guids) {
        return guids
                .stream()
                .filter(ifGuidExistInDb())
                .map(getFromDbAndConvertToDto())
                .collect(Collectors.toList());
    }

    @NotNull
    private List<String> collectGuidsFrom(List<SyndEntry> entries) {
        return entries.stream()
                .limit(RSS_FEED_SIZE_THRESHOLD)
                .map(SyndEntry::getUri)
                .collect(Collectors.toList());
    }

    @NotNull
    private Predicate<String> ifGuidExistInDb() {
        return item -> repository.findByGuid(item) != null;
    }


    private Function<String, NewsItemDto> getFromDbAndConvertToDto() {
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
