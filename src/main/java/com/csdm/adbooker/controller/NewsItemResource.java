package com.csdm.adbooker.controller;

import com.csdm.adbooker.service.GraphQLService;
import graphql.ExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/rest/news")
@RestController
public class NewsItemResource {

    @Autowired
    private GraphQLService graphQLService;

    @PostMapping
    public ResponseEntity<Object> getAllNewsItems(@RequestBody String query) {
        ExecutionResult execute = graphQLService.getGraphQLService().execute(query);

        return new ResponseEntity<>(execute, HttpStatus.OK);
    }

}
