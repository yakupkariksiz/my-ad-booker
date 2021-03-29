package com.csdm.adbooker.service;

import com.csdm.adbooker.model.NewsItem;
import com.csdm.adbooker.model.NewsItemDto;
import com.csdm.adbooker.repository.NewsItemRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.csdm.adbooker.model.NewsItemParameters.NEWS_FEED_URL;
import static com.csdm.adbooker.model.NewsItemParameters.RSS_FEED_INTERVAL_TIME;
import static com.csdm.adbooker.model.NewsItemParameters.RSS_FEED_SIZE_THRESHOLD;

@Slf4j
@EnableScheduling
@Service
public class NewsItemService {

    @Autowired
    private NewsItemRepository repository;

    @Scheduled(fixedRate = RSS_FEED_INTERVAL_TIME)
    @Transactional
    public void fetchRssFeeds() {
        log.info("Started fetching rss feeds at {}", LocalDateTime.now());

        List<SyndEntry> entries = makeHttpRequestAndGetRssEntries();
        List<String> latestGuidsFromRssFeeds = collectGuidsFrom(entries);

        List<NewsItemDto> newsItemsFromDb = getNewsItemsFromDbBy(latestGuidsFromRssFeeds);
        List<NewsItemDto> newsItemsFromRss = convertRssEntryToDto(entries);

        Map<String, NewsItemDto> newsItemsMapFromDb = createMapByItemsFrom(newsItemsFromDb);
        Map<String, NewsItemDto> newsItemsMapFromRss = createMapByItemsFrom(newsItemsFromRss);

        updateItemsInDbByRssFeeds(latestGuidsFromRssFeeds, newsItemsMapFromDb, newsItemsMapFromRss);
        saveNewItemsInDb(latestGuidsFromRssFeeds, newsItemsMapFromDb, newsItemsMapFromRss);

        log.info("Finished fetching rss feeds at {}", LocalDateTime.now());
    }

    private void saveNewItemsInDb(List<String> latestGuidsFromRssFeeds, Map<String, NewsItemDto> newsItemsMapFromDb, Map<String, NewsItemDto> newsItemsMapFromRss) {
        List<NewsItem> entities = latestGuidsFromRssFeeds.stream()
                .filter(ifItemExistsInDb(newsItemsMapFromDb))
                .map(getNewsItemFunction(newsItemsMapFromRss))
                .collect(Collectors.toList());

        repository.saveAll(entities);
    }

    private void updateItemsInDbByRssFeeds(List<String> latestGuidsFromRssFeeds, Map<String, NewsItemDto> newsItemsMapFromDb, Map<String, NewsItemDto> newsItemsMapFromRss) {
        latestGuidsFromRssFeeds.stream()
                .filter(ifItemDoesNotExistInDb(newsItemsMapFromDb))
                .forEach(doUpdateIfRequired(newsItemsMapFromDb, newsItemsMapFromRss));
    }

    private Consumer<String> doUpdateIfRequired(Map<String, NewsItemDto> newsItemsMapFromDb, Map<String, NewsItemDto> newsItemsMapFromRss) {
        return guid -> updateItemIfRequired(newsItemsMapFromDb.get(guid), newsItemsMapFromRss.get(guid));
    }

    private Predicate<String> ifItemExistsInDb(Map<String, NewsItemDto> newsItemsMapFromDb) {
        return guid -> newsItemsMapFromDb.get(guid) == null;
    }

    private Predicate<String> ifItemDoesNotExistInDb(Map<String, NewsItemDto> newsItemsMapFromDb) {
        return guid -> newsItemsMapFromDb.get(guid) != null;
    }

    private Function<String, NewsItem> getNewsItemFunction(Map<String, NewsItemDto> newsItemsMapFromRss) {
        return guid -> NewsItem.builder().title(newsItemsMapFromRss.get(guid).getTitle())
                .guid(newsItemsMapFromRss.get(guid).getGuid())
                .description(newsItemsMapFromRss.get(guid).getDescription())
                .publishedDate(newsItemsMapFromRss.get(guid).getPublishedDate())
                .imageUrl(newsItemsMapFromRss.get(guid).getImageUrl()).build();
    }

    private Map<String, NewsItemDto> createMapByItemsFrom(List<NewsItemDto> newsItemsFromDb) {
        return newsItemsFromDb.stream()
                .collect(Collectors.toMap(
                        NewsItemDto::getGuid,
                        Function.identity()));
    }

    private void updateItemIfRequired(NewsItemDto newsItemFromDb, NewsItemDto newsItemFromRss) {
        NewsItem willBeUpdated = repository.findByGuid(newsItemFromDb.getGuid());

        updateTitleIfChanged(newsItemFromDb.getTitle(), newsItemFromRss.getTitle(), willBeUpdated);
        updateDescriptionIfChanged(newsItemFromDb.getDescription(), newsItemFromRss.getDescription(), willBeUpdated);
        updatePublishedDateIfChanged(newsItemFromDb.getPublishedDate(), newsItemFromRss.getPublishedDate(), willBeUpdated);
        updateImageUrlIfChanged(newsItemFromDb.getImageUrl(), newsItemFromRss.getImageUrl(), willBeUpdated);
    }

    private void updateTitleIfChanged(String titleFromDb, String titleFromRss, NewsItem willBeUpdated) {
        if (!titleFromDb.equals(titleFromRss)) {
            willBeUpdated.setTitle(titleFromRss);
        }
    }

    private void updateDescriptionIfChanged(String descriptionFromDb,
                                            String descriptionFromRss, NewsItem willBeUpdated) {
        if (!descriptionFromDb.equals(descriptionFromRss)) {
            willBeUpdated.setDescription(descriptionFromRss);
        }
    }

    private void updatePublishedDateIfChanged(LocalDateTime publishedDateFromDb,
                                              LocalDateTime publishedDateFromRss, NewsItem willBeUpdated) {
        if (!publishedDateFromDb.equals(publishedDateFromRss)) {
            willBeUpdated.setPublishedDate(publishedDateFromRss);
        }
    }

    private void updateImageUrlIfChanged(String imageUrlFromDb,
                                         String imageUrlFromRss, NewsItem willBeUpdated) {
        if (!imageUrlFromDb.equals(imageUrlFromRss)) {
            willBeUpdated.setImageUrl(imageUrlFromRss);
        }
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

    private List<SyndEntry> makeHttpRequestAndGetRssEntries() {
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
