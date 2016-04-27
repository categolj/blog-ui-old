package am.ik.blog.ui;

import am.ik.marked4j.Marked;
import am.ik.marked4j.MarkedBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Slf4j
@EnableDiscoveryClient
@EnableCircuitBreaker
public class BlogUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogUiApplication.class, args);
    }

    @Bean
    Marked marked() {
        return new MarkedBuilder()
                .breaks(true)
                .build();
    }

    @LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
