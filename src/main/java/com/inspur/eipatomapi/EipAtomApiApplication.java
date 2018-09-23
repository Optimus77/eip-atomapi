package com.inspur.eipatomapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com"})
public class EipAtomApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EipAtomApiApplication.class, args);
    }
}
