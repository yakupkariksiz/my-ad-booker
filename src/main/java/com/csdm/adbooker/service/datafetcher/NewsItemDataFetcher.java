package com.csdm.adbooker.service.datafetcher;

import com.csdm.adbooker.model.NewsItem;
import com.csdm.adbooker.model.NewsItemDto;
import com.csdm.adbooker.repository.NewsItemRepository;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NewsItemDataFetcher implements DataFetcher<NewsItemDto> {

    @Autowired
    private NewsItemRepository repository;

    @Override
    public NewsItemDto get(DataFetchingEnvironment dataFetchingEnvironment) {
        String uri = dataFetchingEnvironment.getArgument("uri");
        NewsItem newsItem = repository.findByGuid(uri);
        return NewsItemDto.builder()
                .guid(newsItem.getGuid())
                .title(newsItem.getTitle())
                .description(newsItem.getDescription())
                .publishedDate(newsItem.getPublishedDate())
                .imageUrl(newsItem.getImageUrl())
                .build();
    }
}
