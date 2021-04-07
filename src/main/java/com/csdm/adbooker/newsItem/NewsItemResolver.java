package com.csdm.adbooker.newsItem;

import com.csdm.adbooker.newsItem.model.NewsItem;
import com.csdm.adbooker.newsItem.model.NewsItemDto;
import graphql.kickstart.tools.GraphQLQueryResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewsItemResolver implements GraphQLQueryResolver {

    @Autowired
    private NewsItemRepository repository;

    public NewsItemDto newsItem(String guid) {
        NewsItem itemFromDb = repository.findByGuid(guid);
        return NewsItemDto.builder().guid(itemFromDb.getGuid())
                .title(itemFromDb.getTitle())
                .description(itemFromDb.getDescription())
                .publishedDate(itemFromDb.getPublishedDate())
                .imageUrl(itemFromDb.getImageUrl())
                .build();
    }

    public List<NewsItemDto> allNewsItems() {
        List<NewsItem> allNewsItem = repository.findAll();

        return allNewsItem.stream().map(item ->
                NewsItemDto.builder()
                        .guid(item.getGuid())
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .publishedDate(item.getPublishedDate())
                        .imageUrl(item.getImageUrl())
                        .build())
                .sorted(Comparator.comparing(NewsItemDto::getPublishedDate).reversed())
                .collect(Collectors.toList());
    }
}
