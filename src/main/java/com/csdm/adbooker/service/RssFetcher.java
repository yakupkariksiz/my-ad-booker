package com.csdm.adbooker.service;

import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.csdm.adbooker.model.NewsItemParameters.TIMEOUT_GETTING_RSS_FEEDS_SECONDS;

@Slf4j
@Service
public class RssFetcher {

    public List<SyndEntry> makeHttpRequestAndGetRssEntries() {
        List<SyndEntry> syndEntries = null;
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<List<SyndEntry>> future = executorService.submit(new GettingRssEntriesCallable());
            syndEntries = future.get(TIMEOUT_GETTING_RSS_FEEDS_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return syndEntries;
    }
}
