package com.sharepay;

import com.sharepay.config.JwtProperties;
import com.sharepay.config.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SharePayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SharePayApplication.class, args);
    }
}
