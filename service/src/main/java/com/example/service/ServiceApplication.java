package com.example.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Objects;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener(CustomerService cs) {
        return event -> cs.all().forEach(System.out::println);
    }

}

@Controller
@ResponseBody
class CustomerHttpController {

    private final CustomerService customerService;
    private final ObservationRegistry observationRegistry;


    public CustomerHttpController(CustomerService customerService, ObservationRegistry observationRegistry) {
        this.customerService = customerService;
        this.observationRegistry = observationRegistry;
    }

    @GetMapping("/customers")
    Collection<Customer> all() {
        return customerService.all();
    }

    @GetMapping("/customers/{id}")
    Customer byId(@PathVariable Integer id) {
        return Observation.createNotStarted("byId", this.observationRegistry).observe(() -> this.customerService.byId(id));
    }

}

@ControllerAdvice
class ErrorHandlingControllerAdvice {

    @ExceptionHandler
    public ProblemDetail handleEmptyResultException(EmptyResultDataAccessException e) {
        var pd = ProblemDetail.forStatus(HttpStatusCode.valueOf(404));
        pd.setDetail("Id must be in the DB");
        return pd;
    }

}


@Service
class CustomerService {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Customer> customerRowMapper = (rs, rowNum) -> new Customer(rs.getInt("id"), rs.getString("name"));

    public CustomerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Customer byId(Integer id) {
        return this.jdbcTemplate.queryForObject("select * from customer where id=?", this.customerRowMapper, id);
    }

    Collection<Customer> all() {
        return this.jdbcTemplate.query("select * from customer", this.customerRowMapper);
    }


}


record Customer(Integer id, String name) {
    public Customer {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
    }
}
