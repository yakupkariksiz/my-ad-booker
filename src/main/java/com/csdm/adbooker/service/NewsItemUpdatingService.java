package com.csdm.adbooker.service;

import com.csdm.adbooker.model.NewsItem;
import com.csdm.adbooker.model.NewsItemDto;
import com.csdm.adbooker.repository.NewsItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Service
public class NewsItemUpdatingService {

    @Autowired
    private NewsItemRepository repository;

    public void updateItemsInDbByRssFeeds(List<String> latestGuidsFromRssFeeds, Map<String, NewsItemDto> newsItemsMapFromDb, Map<String, NewsItemDto> newsItemsMapFromRss) {
        latestGuidsFromRssFeeds.stream()
                .filter(ifItemDoesNotExistInDb(newsItemsMapFromDb))
                .forEach(doUpdateIfRequired(newsItemsMapFromDb, newsItemsMapFromRss));
    }

    private Predicate<String> ifItemDoesNotExistInDb(Map<String, NewsItemDto> newsItemsMapFromDb) {
        return guid -> newsItemsMapFromDb.get(guid) != null;
    }

    private Consumer<String> doUpdateIfRequired(Map<String, NewsItemDto> newsItemsMapFromDb, Map<String, NewsItemDto> newsItemsMapFromRss) {
        return guid -> updateItemIfRequired(newsItemsMapFromDb.get(guid), newsItemsMapFromRss.get(guid));
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
}
