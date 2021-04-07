package com.csdm.adbooker.newsItem.model;

public interface NewsItemParameters {

    static final String NEWS_FEED_URL = "http://feeds.nos.nl/nosjournaal?format=xml";
    static final int RSS_FEED_SIZE_THRESHOLD = 10;
    static final int RSS_FEED_INTERVAL_TIME = 5 * 60 * 1000;
    static final long TIMEOUT_GETTING_RSS_FEEDS_SECONDS = 2;

}
