package com.csdm.adbooker.service;

import com.csdm.adbooker.model.NewsItem;
import com.csdm.adbooker.model.NewsItemDto;
import com.csdm.adbooker.repository.NewsItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class NewsItemSavingService {

    @Autowired
    private NewsItemRepository repository;

    public void saveNewItemsInDb(List<String> latestGuidsFromRssFeeds, Map<String, NewsItemDto> newsItemsMapFromDb, Map<String, NewsItemDto> newsItemsMapFromRss) {
        List<NewsItem> entities = latestGuidsFromRssFeeds.stream()
                .filter(ifItemExistsInDb(newsItemsMapFromDb))
                .map(getNewsItemFunction(newsItemsMapFromRss))
                .collect(Collectors.toList());

        repository.saveAll(entities);
    }

    private Predicate<String> ifItemExistsInDb(Map<String, NewsItemDto> newsItemsMapFromDb) {
        return guid -> newsItemsMapFromDb.get(guid) == null;
    }

    private Function<String, NewsItem> getNewsItemFunction(Map<String, NewsItemDto> newsItemsMapFromRss) {
        return guid -> NewsItem.builder().title(newsItemsMapFromRss.get(guid).getTitle())
                .guid(newsItemsMapFromRss.get(guid).getGuid())
                .description(newsItemsMapFromRss.get(guid).getDescription())
                .publishedDate(newsItemsMapFromRss.get(guid).getPublishedDate())
                .imageUrl(newsItemsMapFromRss.get(guid).getImageUrl()).build();
    }
}
