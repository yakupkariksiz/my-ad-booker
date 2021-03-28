package com.csdm.adbooker.service.datafetcher;

import com.csdm.adbooker.model.NewsItem;
import com.csdm.adbooker.model.NewsItemDto;
import com.csdm.adbooker.repository.NewsItemRepository;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AllNewsItemsDataFetcher implements DataFetcher<List<NewsItemDto>> {

    @Autowired
    private NewsItemRepository repository;

    @Override
    public List<NewsItemDto> get(DataFetchingEnvironment dataFetchingEnvironment) {
        List<NewsItem> allNewsItem = repository.findAll();
        return allNewsItem.stream().map(item ->
                NewsItemDto.builder()
                        .guid(item.getGuid())
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .publishedDate(item.getPublishedDate())
                        .imageUrl(item.getImageUrl())
                        .build()).collect(Collectors.toList());
    }
}
