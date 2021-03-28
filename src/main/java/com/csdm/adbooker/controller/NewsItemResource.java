package com.csdm.adbooker.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/rest/news")
@RestController
public class NewsItemResource {

    @PostMapping
    public void getAllNewsItems(@RequestBody String query) {

    }

}
