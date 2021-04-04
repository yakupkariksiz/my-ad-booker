package com.csdm.adbooker.service;

import com.csdm.adbooker.model.NewsItem;
import com.csdm.adbooker.model.NewsItemDto;
import com.csdm.adbooker.repository.NewsItemRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.csdm.adbooker.model.NewsItemParameters.RSS_FEED_SIZE_THRESHOLD;

@Service
public class NewsItemConverter {

    @Autowired
    private NewsItemRepository repository;

    public Map<String, NewsItemDto> createMapByItemsFrom(List<NewsItemDto> newsItemsFromDb) {
        return newsItemsFromDb.stream()
                .collect(Collectors.toMap(
                        NewsItemDto::getGuid,
                        Function.identity()));
    }

    @NotNull
    public List<NewsItemDto> convertRssEntryToDto(List<SyndEntry> entries) {
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
    public List<NewsItemDto> getNewsItemsFromDbBy(List<String> guids) {
        return guids
                .stream()
                .filter(ifGuidExistInDb())
                .map(getFromDbAndConvertToDto())
                .collect(Collectors.toList());
    }

    @NotNull
    public List<String> collectGuidsFrom(List<SyndEntry> entries) {
        return entries.stream()
                .limit(RSS_FEED_SIZE_THRESHOLD)
                .map(SyndEntry::getUri)
                .collect(Collectors.toList());
    }

    public Function<String, NewsItemDto> getFromDbAndConvertToDto() {
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

    @NotNull
    private Predicate<String> ifGuidExistInDb() {
        return item -> repository.findByGuid(item) != null;
    }
}
