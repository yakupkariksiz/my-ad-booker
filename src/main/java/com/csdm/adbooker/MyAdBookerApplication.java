package com.csdm.adbooker;

import com.csdm.adbooker.service.NewsItemService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MyAdBookerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyAdBookerApplication.class, args);
    }


    @Bean
    CommandLineRunner runner(NewsItemService service) {
        return args -> {
            service.fetchRssFeeds();
        };
    }

}
