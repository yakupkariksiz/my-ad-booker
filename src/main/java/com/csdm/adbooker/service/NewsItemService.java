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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        List<String> guids = collectGuidsFrom(entries);

        List<NewsItemDto> newsItemsFromDb = getNewsItemsFromDbBy(guids);
        List<NewsItemDto> newsItemsFromRss = convertRssEntriesDtos(entries);

        Map<String, NewsItemDto> newsItemsMapFromDb = newsItemsFromDb.stream()
                .collect(Collectors.toMap(
                        NewsItemDto::getGuid,
                        Function.identity()));
        Map<String, NewsItemDto> newsItemsMapFromRss = newsItemsFromRss.stream()
                .collect(Collectors.toMap(
                        NewsItemDto::getGuid,
                        Function.identity()));

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
                updateItem(newsItemFromDb, newsItemFromRss);
            }
        }
        repository.saveAll(entities);
        log.info("Finished fetching rss feeds at {}", LocalDateTime.now());
    }

    private void updateItem(NewsItemDto newsItemFromDb, NewsItemDto newsItemFromRss) {
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
                                         String imageUrlFromRss,NewsItem willBeUpdated) {
        if (!imageUrlFromDb.equals(imageUrlFromRss)) {
            willBeUpdated.setImageUrl(imageUrlFromRss);
        }
    }

    @NotNull
    private List<NewsItemDto> convertRssEntriesDtos(List<SyndEntry> entries) {
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
