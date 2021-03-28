package com.csdm.adbooker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class GraphQLService {

    @Value("classpath:news.graphql")
    Resource resource;

}
