# my-ad-booker

Java development assignment
Create a Spring Boot application that polls a RSS feed every 5 minutes, and stores any
changes in a inmemory database like h2.
The news feed to use is: http://feeds.nos.nl/nosjournaal?format=xml.
Create a data model to store at least the last 10 news items in the database. For each item
at least the title, description, publication date and image needs to be stored. It could be an
item is updated in the feed, in which case its record in the database should be updated as
well.
Expose the news items using GraphQL, see https://github.com/graphql-java-
kickstart/graphql-springboot.
Additional information for GraphQL: https://github.com/graphql-java-kickstart/graphql-spring-
boot/tree/master/example-graphql-tools
Also include graphiql-spring-boot-starter for exposing the GraphiQL IDE to interact with the
GraphQL API.
Deliver a (small) readme file along with the source code which describes how to set up and
run the
application.
Preferably, the source code is shared using an online repository such as github or bitbucket.
