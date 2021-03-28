package com.csdm.adbooker.service;

import com.csdm.adbooker.service.datafetcher.AllNewsItemsDataFetcher;
import com.csdm.adbooker.service.datafetcher.NewsItemDataFetcher;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Service
public class GraphQLService {

    private final AllNewsItemsDataFetcher allNewsItemsDataFetcher;
    private final NewsItemDataFetcher newsItemDataFetcher;
    
    @Value("classpath:news.graphql")
    Resource resource;

    private GraphQL graphQLService;

    public GraphQLService(AllNewsItemsDataFetcher allNewsItemsDataFetcher, NewsItemDataFetcher newsItemDataFetcher) {
        this.allNewsItemsDataFetcher = allNewsItemsDataFetcher;
        this.newsItemDataFetcher = newsItemDataFetcher;
    }

    @PostConstruct
    private void loadSchema() throws IOException {
        File schemaFile = resource.getFile();
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schemaFile);
        RuntimeWiring wiring = buildRuntimeWiring();
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
        graphQLService = GraphQL.newGraphQL(schema).build();
    }

    private RuntimeWiring buildRuntimeWiring() {
        return RuntimeWiring
                .newRuntimeWiring()
                .type("Query", typeWiring -> typeWiring
                        .dataFetcher("allNewsItems", allNewsItemsDataFetcher)
                        .dataFetcher("newsItem", newsItemDataFetcher))
                .build();
    }

    public GraphQL getGraphQLService() {
        return graphQLService;
    }
}
