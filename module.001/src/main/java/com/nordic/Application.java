package com.nordic;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Slf4j
@EnableFeignClients
@EnableEurekaClient
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableJpaRepositories
@EnableConfigurationProperties()
public class Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
                .listeners(new BootstrapContextOpenListener()).run(args);
    }
}