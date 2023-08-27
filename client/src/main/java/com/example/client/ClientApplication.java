package com.example.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener(CustomerClient customerClient) {
        return event -> customerClient.all().subscribe(System.out::println);
    }

    @Bean
    CustomerClient customerClient(WebClient.Builder builder) {
        return HttpServiceProxyFactory.builder(WebClientAdapter.forClient(builder.baseUrl("http://localhost:8080").build())).build().createClient(CustomerClient.class);
    }

    @Bean
    RouteLocator gateway(RouteLocatorBuilder rlb) {
        return rlb.routes().route(rs -> rs.path("/proxy").filters(fs -> fs.setPath("/customers").retry(3).addResponseHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")).uri("http://localhost:8080")).build();
    }

}

@Controller
class CustomerGraphqlController {

    private final CustomerClient customerClient;

    public CustomerGraphqlController(CustomerClient customerClient) {
        this.customerClient = customerClient;
    }

    @QueryMapping
    Flux<Customer> customers() {
        return this.customerClient.all();
    }

    @QueryMapping
    Mono<Customer> byId(@Argument Integer id) {
        return this.customerClient.byId(id);
    }

    @SchemaMapping(typeName = "Customer")
    Mono<Profile> profile(Customer customer) {
        return Mono.just(new Profile(customer.id()));
    }

}

interface CustomerClient {

    @GetExchange("/customers")
    Flux<Customer> all();

    @GetExchange("/customers/{id}")
    Mono<Customer> byId(@PathVariable Integer id);

}

record Profile(Integer id) {

}

record Customer(Integer id, String name) {

}
