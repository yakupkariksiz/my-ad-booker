# my-ad-booker

This application is used to poll latest news from [link to Nos Journaal!](http://feeds.nos.nl/nosjournaal?format=xml) as rss feeds.


Technologies: Java SE 11, Spring Boot 2.4.4, Spring Web, Spring Data JPA, Lombok, H2, Rome Framework, GraphQL.

## Instructions to Run the Application

1. Download the my-ad-booker application and go to the root path. 
2. Execute `mvn clean package` command in the terminal to build the application. 
3. Type `java -jar target/my-ad-booker-0.0.1-SNAPSHOT.jar` and execute it in the terminal to start up the application.
4. Open the [GraphiQL IDE](http://localhost:8080/graphiql) to query api based on provided **Queries**.

### Example Query
```
{
  allNewsItems {
    title
    description
    imageUrl
    guid
    publishedDate
  }
}

{
  newsItem(guid: "https://nos.nl/l/2374830"){
    guid
    title
    publishedDate
    imageUrl
    description
  }
}
```
