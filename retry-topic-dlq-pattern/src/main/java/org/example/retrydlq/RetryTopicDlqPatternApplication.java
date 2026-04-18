package org.example.retrydlq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RetryTopicDlqPatternApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetryTopicDlqPatternApplication.class, args);
    }
}
