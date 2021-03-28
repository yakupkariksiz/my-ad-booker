package com.csdm.adbooker.repository;

import com.csdm.adbooker.model.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    public NewsItem findByGuid(String guid);

}
