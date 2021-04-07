package com.csdm.adbooker.newsItem;

import com.csdm.adbooker.newsItem.model.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    public NewsItem findByGuid(String guid);

}
